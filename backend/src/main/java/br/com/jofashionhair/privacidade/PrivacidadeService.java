package br.com.jofashionhair.privacidade;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Cliente;
import br.com.jofashionhair.domain.ConfiguracaoSalao;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.repository.AtendimentoWhatsappEstadoRepository;
import br.com.jofashionhair.repository.ClienteRepository;
import br.com.jofashionhair.repository.ConfiguracaoSalaoRepository;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.CatalogoAlteradoEvent;
import br.com.jofashionhair.service.RelogioSalao;

/**
 * Direitos da cliente (LGPD) e retenção de dados.
 *
 * <ul>
 *   <li><b>Acesso:</b> exportar tudo o que o sistema guarda sobre ela.</li>
 *   <li><b>Exclusão:</b> apaga a cliente, todos os agendamentos dela e a conversa
 *       do WhatsApp. Definitivo: ela some do painel.</li>
 *   <li><b>Retenção:</b> todo mês, exclui quem está sem atendimento há mais
 *       que o prazo definido no painel (padrão 24 meses).</li>
 * </ul>
 */
@Service
public class PrivacidadeService {

    private static final Logger log = LoggerFactory.getLogger(PrivacidadeService.class);
    private static final int DIAS_GUARDAR_CONVERSAS = 90;

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AtendimentoWhatsappEstadoRepository conversaRepository;
    private final ConfiguracaoSalaoRepository configuracaoRepository;
    private final RelogioSalao relogio;
    private final ApplicationEventPublisher eventos;

    public PrivacidadeService(ClienteRepository clienteRepository,
                              AgendamentoRepository agendamentoRepository,
                              AtendimentoWhatsappEstadoRepository conversaRepository,
                              ConfiguracaoSalaoRepository configuracaoRepository,
                              RelogioSalao relogio,
                              ApplicationEventPublisher eventos) {
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.conversaRepository = conversaRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.relogio = relogio;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public ExportacaoCliente exportar(Long clienteId) {
        Cliente c = buscar(clienteId);
        List<AgendamentoResumo> agendamentos = agendamentoRepository.findDoClienteComDetalhes(clienteId).stream()
                .map(a -> AgendamentoResumo.de(a, relogio))
                .toList();
        return new ExportacaoCliente(relogio.agoraOffset(), c.getId(), c.getNome(), c.getTelefone(), c.getEmail(),
                c.getObservacoes(), agendamentos);
    }

    @Transactional
    public void excluir(Long clienteId) {
        Cliente c = buscar(clienteId);
        // sem telefone, os lembretes dos horários futuros não teriam para onde ir
        if (c.getTelefone() != null && !agendamentoRepository.findFuturosDoTelefone(c.getTelefone(),
                List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO), relogio.agoraOffset()).isEmpty()) {
            throw new RegraNegocioException(
                    "Esta cliente tem agendamentos futuros. Cancele-os na Agenda antes de excluir.");
        }
        excluirCliente(c);
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
    }

    /** Dia 1 de cada mês, às 4h: aplica o prazo de retenção e limpa conversas antigas do robô. */
    @Scheduled(cron = "0 0 4 1 * *", zone = "America/Sao_Paulo")
    @Transactional
    public void aplicarRetencao() {
        OffsetDateTime agora = relogio.agoraOffset();
        int meses = configuracaoRepository.findById(ConfiguracaoSalao.ID_UNICO)
                .map(ConfiguracaoSalao::getRetencaoMeses)
                .orElse(24);
        List<Long> ids = clienteRepository.idsSemAtendimentoDesde(agora.minusMonths(meses));
        ids.forEach(id -> clienteRepository.findById(id).ifPresent(this::excluirCliente));
        int conversas = conversaRepository.apagarInativasDesde(agora.minusDays(DIAS_GUARDAR_CONVERSAS));
        log.info("Retenção LGPD: {} cliente(s) excluída(s), {} conversa(s) antiga(s) apagada(s)",
                ids.size(), conversas);
    }

    /** Apaga conversa, agendamentos e a própria cliente. */
    private void excluirCliente(Cliente c) {
        if (c.getTelefone() != null) {
            conversaRepository.apagarPorTelefone(c.getTelefone());
        }
        agendamentoRepository.apagarDoCliente(c.getId());
        clienteRepository.delete(c);
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrada."));
    }
}
