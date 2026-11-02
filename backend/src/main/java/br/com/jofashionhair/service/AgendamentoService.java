package br.com.jofashionhair.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Agendamento;
import br.com.jofashionhair.domain.Cliente;
import br.com.jofashionhair.domain.Disponibilidade;
import br.com.jofashionhair.domain.OrigemAgendamento;
import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.domain.Telefone;
import br.com.jofashionhair.exception.HorarioIndisponivelException;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.repository.ClienteRepository;
import br.com.jofashionhair.repository.ProfissionalRepository;

/**
 * Reserva atômica de horários e ciclo de vida dos agendamentos,
 * usado pelos três canais (site, WhatsApp e /admin).
 *
 * <p>Proteção em duas camadas contra double booking:</p>
 * <ol>
 *   <li><b>Lock pessimista</b> na linha da profissional (SELECT ... FOR UPDATE):
 *       reservas concorrentes para a MESMA profissional esperam na fila, então a
 *       verificação (expediente, bloqueios, conflitos) e a gravação acontecem
 *       sem ninguém "furar" no meio.</li>
 *   <li><b>Constraint de exclusão</b> no PostgreSQL: rede de segurança final.</li>
 * </ol>
 */
@Service
public class AgendamentoService {

    /** SQLSTATE do PostgreSQL para violação de EXCLUDE constraint. */
    private static final String EXCLUSION_VIOLATION = "23P01";

    private final AgendamentoRepository agendamentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ClienteRepository clienteRepository;
    private final DisponibilidadeService disponibilidade;
    private final RelogioSalao relogio;
    private final ApplicationEventPublisher eventos;
    private final br.com.jofashionhair.promocao.PromocaoService promocaoService;
    private final br.com.jofashionhair.pagamento.PixService pixService;
    private final SalaoService salaoService;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              ProfissionalRepository profissionalRepository,
                              ClienteRepository clienteRepository,
                              DisponibilidadeService disponibilidade,
                              RelogioSalao relogio,
                              ApplicationEventPublisher eventos,
                              br.com.jofashionhair.promocao.PromocaoService promocaoService,
                              br.com.jofashionhair.pagamento.PixService pixService,
                              SalaoService salaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.profissionalRepository = profissionalRepository;
        this.clienteRepository = clienteRepository;
        this.disponibilidade = disponibilidade;
        this.relogio = relogio;
        this.eventos = eventos;
        this.promocaoService = promocaoService;
        this.pixService = pixService;
        this.salaoService = salaoService;
    }

    /**
     * Reserva um horário.
     *
     * @throws HorarioIndisponivelException se o horário não puder ser reservado
     *                                      (com o motivo e o contexto para sugestões)
     */
    @Transactional
    public AgendamentoResumo reservar(SolicitacaoAgendamento cmd) {
        Servico servico = disponibilidade.buscarServicoAtivo(cmd.servicoId());
        LocalDateTime inicio = cmd.inicio().truncatedTo(ChronoUnit.MINUTES);
        boolean ignorarAntecedencia = cmd.origem() == OrigemAgendamento.ADMIN;

        Profissional profissional = cmd.profissionalId() != null
                ? travarProfissionalEspecifica(cmd.profissionalId(), servico, inicio, ignorarAntecedencia)
                : travarPrimeiraProfissionalLivre(servico, inicio, ignorarAntecedencia);

        Cliente cliente = obterOuCriarCliente(cmd.nomeCliente(), cmd.telefone());

        // dia de promoção: o desconto já entra no preço cobrado
        Agendamento agendamento = Agendamento.criar(cliente, profissional, servico,
                relogio.paraOffset(inicio), cmd.origem(), cmd.observacoes(),
                promocaoService.precoCom(servico, inicio.toLocalDate()));

        // serviço com sinal (site e WhatsApp): o horário fica segurado até o Pix ser pago
        if (servico.isExigeSinal() && cmd.origem() != OrigemAgendamento.ADMIN && pixService.configurado()) {
            java.math.BigDecimal sinal = servico.getValorSinal().min(agendamento.getPrecoCobrado());
            if (sinal.signum() > 0) {
                int prazo = salaoService.informacoes().sinalPrazoMinutos();
                agendamento.aguardarSinal(sinal, relogio.agoraOffset().plusMinutes(prazo));
            }
        }
        try {
            agendamentoRepository.saveAndFlush(agendamento);
        } catch (DataIntegrityViolationException e) {
            if (violouConstraintDeConflito(e)) {
                throw new HorarioIndisponivelException(Disponibilidade.HORARIO_OCUPADO,
                        servico.getId(), cmd.profissionalId(), inicio);
            }
            throw e;
        }
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
        return AgendamentoResumo.de(agendamento, relogio);
    }

    @Transactional(readOnly = true)
    public AgendamentoResumo buscar(Long id) {
        return AgendamentoResumo.de(carregar(id), relogio);
    }

    /**
     * Muda o status (confirmar, concluir, cancelar, marcar falta).
     * CANCELADO e FALTOU liberam o horário na hora para o site e o robô.
     */
    @Transactional
    public AgendamentoResumo alterarStatus(Long id, StatusAgendamento destino) {
        Agendamento agendamento = carregar(id);
        if (destino == StatusAgendamento.CONFIRMADO && agendamento.getStatus() == StatusAgendamento.AGUARDANDO_SINAL) {
            agendamento.registrarSinalPago(relogio.agoraOffset());
        } else if (destino == StatusAgendamento.CONFIRMADO && agendamento.getStatus() == StatusAgendamento.PENDENTE) {
            agendamento.confirmarPeloCliente(relogio.agoraOffset());
        } else {
            agendamento.alterarStatus(destino);
        }
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
        return AgendamentoResumo.de(agendamento, relogio);
    }

    // ------------------------------------------------------------------ ações do próprio cliente (robô)

    /** Próximos agendamentos ativos do cliente (PENDENTE ou CONFIRMADO). */
    @Transactional(readOnly = true)
    public List<AgendamentoResumo> futurosDoCliente(String telefone) {
        return agendamentoRepository.findFuturosDoTelefone(Telefone.normalizar(telefone),
                        List.of(StatusAgendamento.AGUARDANDO_SINAL, StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO),
                        relogio.agoraOffset())
                .stream()
                .map(a -> AgendamentoResumo.de(a, relogio))
                .toList();
    }

    /** Cliente respondeu "sim" ao lembrete. Só funciona para agendamento do próprio telefone. */
    @Transactional
    public AgendamentoResumo confirmarPeloCliente(Long id, String telefone) {
        Agendamento agendamento = carregarDoCliente(id, telefone);
        agendamento.confirmarPeloCliente(relogio.agoraOffset());
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));   // painel atualiza ao vivo
        return AgendamentoResumo.de(agendamento, relogio);
    }

    /** Cliente cancelou pelo WhatsApp. O horário é liberado na hora. */
    @Transactional
    public AgendamentoResumo cancelarPeloCliente(Long id, String telefone) {
        Agendamento agendamento = carregarDoCliente(id, telefone);
        agendamento.alterarStatus(StatusAgendamento.CANCELADO);
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
        return AgendamentoResumo.de(agendamento, relogio);
    }

    // ------------------------------------------------------------------ privados

    private Agendamento carregarDoCliente(Long id, String telefone) {
        Agendamento agendamento = carregar(id);
        if (!agendamento.getCliente().getTelefone().equals(Telefone.normalizar(telefone))) {
            // não revela que o agendamento existe para outro número
            throw new RecursoNaoEncontradoException("Agendamento não encontrado.");
        }
        return agendamento;
    }

    private Agendamento carregar(Long id) {
        return agendamentoRepository.findByIdComDetalhes(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado."));
    }

    private Profissional travarProfissionalEspecifica(Long profissionalId, Servico servico,
                                                      LocalDateTime inicio, boolean ignorarAntecedencia) {
        Profissional profissional = profissionalRepository.findByIdParaReserva(profissionalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrada."));

        Disponibilidade resultado = disponibilidade.verificar(profissional, servico, inicio, ignorarAntecedencia);
        if (resultado != Disponibilidade.DISPONIVEL) {
            throw new HorarioIndisponivelException(resultado, servico.getId(), profissionalId, inicio);
        }
        return profissional;
    }

    /**
     * "Sem preferência": percorre as profissionais que realizam o serviço
     * (sempre na mesma ordem, por id, para evitar deadlock entre transações)
     * e reserva com a primeira livre.
     */
    private Profissional travarPrimeiraProfissionalLivre(Servico servico, LocalDateTime inicio,
                                                         boolean ignorarAntecedencia) {
        List<Profissional> candidatas = profissionalRepository.findAtivasQueRealizam(servico.getId());
        if (candidatas.isEmpty()) {
            throw new HorarioIndisponivelException(Disponibilidade.NAO_REALIZA_SERVICO,
                    servico.getId(), null, inicio);
        }

        boolean todasForaDoExpediente = true;
        for (Profissional candidata : candidatas) {
            Profissional travada = profissionalRepository.findByIdParaReserva(candidata.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrada."));

            Disponibilidade resultado = disponibilidade.verificar(travada, servico, inicio, ignorarAntecedencia);
            if (resultado == Disponibilidade.DISPONIVEL) {
                return travada;
            }
            if (resultado == Disponibilidade.ANTECEDENCIA_INSUFICIENTE
                    || resultado == Disponibilidade.SALAO_FECHADO) {
                // vale para todas: não adianta testar as demais
                throw new HorarioIndisponivelException(resultado, servico.getId(), null, inicio);
            }
            if (resultado != Disponibilidade.FORA_DO_EXPEDIENTE) {
                todasForaDoExpediente = false;
            }
        }
        throw new HorarioIndisponivelException(
                todasForaDoExpediente ? Disponibilidade.FORA_DO_EXPEDIENTE : Disponibilidade.HORARIO_OCUPADO,
                servico.getId(), null, inicio);
    }

    private Cliente obterOuCriarCliente(String nome, String telefoneBruto) {
        String telefone = Telefone.normalizar(telefoneBruto);
        return clienteRepository.findByTelefone(telefone)
                .orElseGet(() -> clienteRepository.save(new Cliente(nome.trim(), telefone)));
    }

    private boolean violouConstraintDeConflito(Throwable erro) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && EXCLUSION_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
            if (t.getMessage() != null && t.getMessage().contains("ex_agendamentos_sem_conflito")) {
                return true;
            }
        }
        return false;
    }
}
