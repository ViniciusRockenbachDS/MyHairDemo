package br.com.jofashionhair.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.config.AppProperties;
import br.com.jofashionhair.domain.Disponibilidade;
import br.com.jofashionhair.domain.HorarioFuncionamento;
import br.com.jofashionhair.domain.HorarioTrabalho;
import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.repository.BloqueioAgendaRepository;
import br.com.jofashionhair.repository.HorarioFuncionamentoRepository;
import br.com.jofashionhair.repository.HorarioTrabalhoRepository;
import br.com.jofashionhair.repository.ProfissionalRepository;
import br.com.jofashionhair.repository.ServicoRepository;

/**
 * Regras de disponibilidade. Tudo aqui lê do banco a cada consulta (duração do
 * serviço, funcionamento do salão, expediente), então o que o dono altera no
 * /admin vale na próxima consulta do site ou do robô.
 *
 * <p>Horário reservável = funcionamento do salão ∩ expediente da profissional
 * − bloqueios − agendamentos existentes.</p>
 */
@Service
@Transactional(readOnly = true)
public class DisponibilidadeService {

    /** Quantos dias à frente procurar alternativas quando o dia pedido está cheio. */
    private static final int DIAS_BUSCA_ALTERNATIVA = 7;

    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final HorarioTrabalhoRepository horarioRepository;
    private final HorarioFuncionamentoRepository funcionamentoRepository;
    private final BloqueioAgendaRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final AppProperties props;
    private final RelogioSalao relogio;

    public DisponibilidadeService(ServicoRepository servicoRepository,
                                  ProfissionalRepository profissionalRepository,
                                  HorarioTrabalhoRepository horarioRepository,
                                  HorarioFuncionamentoRepository funcionamentoRepository,
                                  BloqueioAgendaRepository bloqueioRepository,
                                  AgendamentoRepository agendamentoRepository,
                                  AppProperties props,
                                  RelogioSalao relogio) {
        this.servicoRepository = servicoRepository;
        this.profissionalRepository = profissionalRepository;
        this.horarioRepository = horarioRepository;
        this.funcionamentoRepository = funcionamentoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.props = props;
        this.relogio = relogio;
    }

    public Servico buscarServicoAtivo(Long servicoId) {
        return servicoRepository.findById(servicoId)
                .filter(Servico::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));
    }

    /**
     * Verifica UM horário para UMA profissional, na ordem:
     * ativa → realiza o serviço → antecedência → salão aberto → expediente → bloqueios → conflitos.
     *
     * @param inicio horário local do salão
     */
    public Disponibilidade verificar(Profissional profissional, Servico servico, LocalDateTime inicio) {
        return verificar(profissional, servico, inicio, false);
    }

    /**
     * @param ignorarAntecedencia true para o /admin, que pode encaixar um
     *                            cliente de balcão "agora" ou registrar um
     *                            atendimento que já aconteceu
     */
    public Disponibilidade verificar(Profissional profissional, Servico servico, LocalDateTime inicio,
                                     boolean ignorarAntecedencia) {
        if (!profissional.isAtivo()) {
            return Disponibilidade.PROFISSIONAL_INATIVO;
        }
        if (!profissionalRepository.realizaServico(profissional.getId(), servico.getId())) {
            return Disponibilidade.NAO_REALIZA_SERVICO;
        }

        LocalDateTime fim = inicio.plusMinutes(servico.getDuracaoMinutos());

        if (!ignorarAntecedencia && inicio.isBefore(limiteMinimo())) {
            return Disponibilidade.ANTECEDENCIA_INSUFICIENTE;
        }
        // atendimento que atravessaria a meia-noite nunca está dentro do funcionamento
        if (!fim.toLocalDate().equals(inicio.toLocalDate())) {
            return Disponibilidade.SALAO_FECHADO;
        }
        short diaSemana = (short) inicio.getDayOfWeek().getValue();

        HorarioFuncionamento funcionamento = funcionamentoRepository.findById(diaSemana).orElse(null);
        if (funcionamento == null || !funcionamento.cobre(inicio.toLocalTime(), fim.toLocalTime())) {
            return Disponibilidade.SALAO_FECHADO;
        }
        if (!horarioRepository.cobreIntervalo(profissional.getId(), diaSemana,
                inicio.toLocalTime(), fim.toLocalTime())) {
            return Disponibilidade.FORA_DO_EXPEDIENTE;
        }

        OffsetDateTime ini = relogio.paraOffset(inicio);
        OffsetDateTime fimOffset = relogio.paraOffset(fim);

        if (bloqueioRepository.existeSobreposicao(profissional.getId(), ini, fimOffset)) {
            return Disponibilidade.BLOQUEADO;
        }
        if (agendamentoRepository.existeConflito(profissional.getId(),
                StatusAgendamento.OCUPAM_AGENDA, ini, fimOffset, 0L)) {
            return Disponibilidade.HORARIO_OCUPADO;
        }
        return Disponibilidade.DISPONIVEL;
    }

    /**
     * Horários de início livres de uma profissional num dia.
     * Poucas consultas (funcionamento, expediente, agendamentos e bloqueios do
     * dia) e a grade é calculada em memória, de slot em slot.
     */
    public List<LocalTime> horariosLivres(Profissional profissional, Servico servico, LocalDate data) {
        if (!profissional.isAtivo()
                || !profissionalRepository.realizaServico(profissional.getId(), servico.getId())) {
            return List.of();
        }
        short diaSemana = (short) data.getDayOfWeek().getValue();

        HorarioFuncionamento funcionamento = funcionamentoRepository.findById(diaSemana).orElse(null);
        if (funcionamento == null || funcionamento.isFechado()) {
            return List.of();
        }

        List<HorarioTrabalho> faixas =
                horarioRepository.findByProfissionalIdAndDiaSemanaOrderByHoraInicio(profissional.getId(), diaSemana);
        if (faixas.isEmpty()) {
            return List.of();
        }

        OffsetDateTime inicioDia = relogio.paraOffset(data.atStartOfDay());
        OffsetDateTime fimDia = relogio.paraOffset(data.plusDays(1).atStartOfDay());

        List<Intervalo> ocupados = new ArrayList<>();
        agendamentoRepository.findOcupandoPeriodo(profissional.getId(), StatusAgendamento.OCUPAM_AGENDA,
                        inicioDia, fimDia)
                .forEach(a -> ocupados.add(new Intervalo(
                        relogio.paraLocal(a.getDataInicio()), relogio.paraLocal(a.getDataFim()))));
        bloqueioRepository.findSobrepostos(profissional.getId(), inicioDia, fimDia)
                .forEach(b -> ocupados.add(new Intervalo(
                        relogio.paraLocal(b.getDataInicio()), relogio.paraLocal(b.getDataFim()))));

        int duracao = servico.getDuracaoMinutos();
        int passo = props.agenda().slotMinutos();
        LocalDateTime limite = limiteMinimo();
        TreeSet<LocalTime> livres = new TreeSet<>();

        for (HorarioTrabalho faixa : faixas) {
            // recorta o expediente pelo funcionamento do salão
            LocalTime abre = max(faixa.getHoraInicio(), funcionamento.getHoraAbertura());
            LocalTime fecha = min(faixa.getHoraFim(), funcionamento.getHoraFechamento());
            if (!fecha.isAfter(abre)) {
                continue;
            }
            LocalDateTime cursor = data.atTime(abre);
            LocalDateTime fimFaixa = data.atTime(fecha);

            while (!cursor.plusMinutes(duracao).isAfter(fimFaixa)) {
                Intervalo candidato = new Intervalo(cursor, cursor.plusMinutes(duracao));
                if (!cursor.isBefore(limite) && ocupados.stream().noneMatch(candidato::sobrepoe)) {
                    livres.add(cursor.toLocalTime());
                }
                cursor = cursor.plusMinutes(passo);
            }
        }
        return new ArrayList<>(livres);
    }

    /** Horários livres do dia por profissional (todas, ou só a informada). */
    public List<HorariosProfissional> horariosLivresDoDia(Long servicoId, Long profissionalId, LocalDate data) {
        Servico servico = buscarServicoAtivo(servicoId);
        List<Profissional> profissionais = profissionalId == null
                ? profissionalRepository.findAtivasQueRealizam(servicoId)
                : List.of(profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrada.")));

        return profissionais.stream()
                .map(p -> new HorariosProfissional(p.getId(), p.getNome(), horariosLivres(p, servico, data)))
                .toList();
    }

    /**
     * Sugere horários alternativos: primeiro no próprio dia pedido (os mais
     * próximos do horário desejado, com qualquer profissional que realize o
     * serviço); se o dia estiver cheio, avança dia a dia até 7 dias.
     * Em empate de proximidade, a profissional preferida vem primeiro.
     */
    public List<SugestaoHorario> sugerirAlternativas(Long servicoId, Long profissionalPreferidaId,
                                                     LocalDateTime desejado, int limite) {
        Servico servico = buscarServicoAtivo(servicoId);
        List<Profissional> candidatas = profissionalRepository.findAtivasQueRealizam(servicoId);

        for (int d = 0; d < DIAS_BUSCA_ALTERNATIVA; d++) {
            LocalDate dia = desejado.toLocalDate().plusDays(d);
            List<SugestaoHorario> doDia = new ArrayList<>();
            for (Profissional p : candidatas) {
                for (LocalTime hora : horariosLivres(p, servico, dia)) {
                    doDia.add(new SugestaoHorario(p.getId(), p.getNome(), dia.atTime(hora)));
                }
            }
            if (!doDia.isEmpty()) {
                doDia.sort(Comparator
                        .comparingLong((SugestaoHorario s) ->
                                Math.abs(Duration.between(desejado, s.inicio()).toMinutes()))
                        .thenComparingInt(s ->
                                Objects.equals(s.profissionalId(), profissionalPreferidaId) ? 0 : 1));
                return doDia.stream().limit(limite).toList();
            }
        }
        return List.of();
    }

    private LocalDateTime limiteMinimo() {
        return relogio.agora().plusMinutes(props.agenda().antecedenciaMinimaMinutos());
    }

    private static LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    /** Intervalo semiaberto [inicio, fim). */
    private record Intervalo(LocalDateTime inicio, LocalDateTime fim) {
        boolean sobrepoe(Intervalo outro) {
            return inicio.isBefore(outro.fim) && fim.isAfter(outro.inicio);
        }
    }
}
