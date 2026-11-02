package br.com.jofashionhair.lembrete;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.jofashionhair.config.AppProperties;
import br.com.jofashionhair.domain.Agendamento;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.RelogioSalao;
import br.com.jofashionhair.whatsapp.AtendimentoWhatsappService;
import br.com.jofashionhair.whatsapp.EvolutionApiClient;
import br.com.jofashionhair.whatsapp.MensagensBot;

/**
 * Rotina de lembretes automáticos por WhatsApp (anti no-show).
 *
 * <p>A cada minuto procura agendamentos PENDENTE/CONFIRMADO que entraram na
 * janela do 1º lembrete (padrão 24h antes) ou do 2º (padrão 2h antes) e envia a
 * mensagem. O lembrete pede "1 - Confirmar / 2 - Cancelar"; a resposta é
 * tratada pela máquina de estados do robô (Passo 4):</p>
 * <ul>
 *   <li>1 / sim → agendamento CONFIRMADO, com data da confirmação do cliente</li>
 *   <li>2 / não / cancelar → CANCELADO, horário liberado na hora no site e no robô,
 *       e o robô já oferece remarcar</li>
 * </ul>
 *
 * <p><b>Garantias:</b> cada lembrete é "reservado" com um UPDATE atômico antes
 * do envio (sem duplicidade, mesmo com várias instâncias). Se o envio falhar, a
 * reserva é desfeita e a próxima execução tenta de novo. Nada é enviado no
 * horário de silêncio (madrugada).</p>
 */
@Service
public class LembreteService {

    private static final Logger log = LoggerFactory.getLogger(LembreteService.class);

    private final AgendamentoRepository agendamentoRepository;
    private final EvolutionApiClient evolution;
    private final AtendimentoWhatsappService atendimento;
    private final MensagensBot mensagens;
    private final RelogioSalao relogio;
    private final AppProperties.Lembretes config;
    private final LocalTime silencioInicio;
    private final LocalTime silencioFim;

    public LembreteService(AgendamentoRepository agendamentoRepository,
                           EvolutionApiClient evolution,
                           AtendimentoWhatsappService atendimento,
                           MensagensBot mensagens,
                           RelogioSalao relogio,
                           AppProperties props) {
        this.agendamentoRepository = agendamentoRepository;
        this.evolution = evolution;
        this.atendimento = atendimento;
        this.mensagens = mensagens;
        this.relogio = relogio;
        this.config = props.lembretes();
        this.silencioInicio = LocalTime.parse(config.silencioInicio());
        this.silencioFim = LocalTime.parse(config.silencioFim());
    }

    /** Executa 1 minuto depois do fim da execução anterior (nunca sobrepõe). */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void executar() {
        if (!config.habilitado() || emHorarioDeSilencio(relogio.agora().toLocalTime())) {
            return;
        }
        OffsetDateTime agora = relogio.agoraOffset();

        // 1º lembrete: entre (agora + 2h) e (agora + 24h). Mais perto que isso, quem cuida é o 2º.
        processarJanela("1º lembrete",
                agendamentoRepository.idsParaPrimeiroLembrete(
                        agora.plusHours(config.segundoHoras()), agora.plusHours(config.primeiroHoras()),
                        config.primeiroHoras()),
                id -> agendamentoRepository.marcarPrimeiroLembrete(id, agora),
                agendamentoRepository::desmarcarPrimeiroLembrete,
                a -> true);

        // 2º lembrete: entre (agora + 15 min) e (agora + 2h). Só pede confirmação se o cliente ainda não confirmou.
        processarJanela("2º lembrete",
                agendamentoRepository.idsParaSegundoLembrete(
                        agora.plusMinutes(config.minimoMinutosAntes()), agora.plusHours(config.segundoHoras()),
                        config.segundoHoras()),
                id -> agendamentoRepository.marcarSegundoLembrete(id, agora),
                agendamentoRepository::desmarcarSegundoLembrete,
                a -> a.getConfirmadoClienteEm() == null);
    }

    /**
     * Envio manual pelo /admin ("reenviar lembrete"): sempre pede confirmação.
     * @return true se a mensagem foi entregue à Evolution (ou registrada no log em dev)
     */
    public boolean enviarAgora(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findByIdComDetalhes(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado."));
        if (!StatusAgendamento.OCUPAM_AGENDA.contains(agendamento.getStatus())
                || agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                || agendamento.getStatus() == StatusAgendamento.AGUARDANDO_SINAL) {
            throw new RegraNegocioException("Só é possível lembrar agendamentos pendentes ou confirmados.");
        }
        if (!agendamento.getDataInicio().isAfter(relogio.agoraOffset())) {
            throw new RegraNegocioException("Este agendamento já começou.");
        }
        return enviar(agendamento, true);
    }

    // ------------------------------------------------------------------ privados

    private void processarJanela(String nome, List<Long> ids,
                                 ToIntFunction<Long> marcar,
                                 ToIntFunction<Long> desmarcar,
                                 Function<Agendamento, Boolean> pedirConfirmacao) {
        for (Long id : ids) {
            if (marcar.applyAsInt(id) == 0) {
                continue;   // outra execução já cuidou, ou o status mudou
            }
            boolean ok = false;
            try {
                Agendamento agendamento = agendamentoRepository.findByIdComDetalhes(id).orElse(null);
                ok = agendamento != null && enviar(agendamento, pedirConfirmacao.apply(agendamento));
            } catch (RuntimeException e) {
                log.error("Erro no {} do agendamento {}", nome, id, e);
            }
            if (ok) {
                log.info("{} enviado (agendamento {})", nome, id);
            } else {
                desmarcar.applyAsInt(id);   // tenta de novo na próxima execução
            }
        }
    }

    private boolean enviar(Agendamento agendamento, boolean pedirConfirmacao) {
        AgendamentoResumo resumo = AgendamentoResumo.de(agendamento, relogio);
        String texto = mensagens.lembrete(resumo, pedirConfirmacao, relogio.agora().toLocalDate());

        // em dev (envio desligado) a mensagem vai para o log e conta como enviada
        boolean enviado = evolution.enviarTexto(resumo.telefone(), texto) || !evolution.estaAtivo();
        if (enviado && pedirConfirmacao) {
            // a próxima resposta do cliente ("1"/"2") é interpretada como resposta a este lembrete
            atendimento.aguardarConfirmacaoLembrete(resumo.telefone(), resumo.id(), agendamento.getDataInicio());
        }
        return enviado;
    }

    boolean emHorarioDeSilencio(LocalTime agora) {
        if (silencioInicio.isAfter(silencioFim)) {        // atravessa a meia-noite (21:00 → 07:30)
            return !agora.isBefore(silencioInicio) || agora.isBefore(silencioFim);
        }
        return !agora.isBefore(silencioInicio) && agora.isBefore(silencioFim);
    }
}
