package br.com.jofashionhair.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import br.com.jofashionhair.service.AgendamentoResumo;

/**
 * Avisos enviados por iniciativa do sistema (não são resposta a uma mensagem).
 * Rodam em segundo plano (@Async): se a Evolution estiver lenta ou fora do ar,
 * o agendamento no site continua instantâneo.
 */
@Component
public class NotificacaoWhatsapp {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoWhatsapp.class);

    private final EvolutionApiClient evolution;
    private final MensagensBot mensagens;

    public NotificacaoWhatsapp(EvolutionApiClient evolution, MensagensBot mensagens) {
        this.evolution = evolution;
        this.mensagens = mensagens;
    }

    @Async
    public void sinalRecebido(AgendamentoResumo reserva) {
        enviar(reserva, mensagens.sinalRecebido(reserva));
    }

    @Async
    public void sinalExpirado(AgendamentoResumo reserva) {
        enviar(reserva, mensagens.sinalExpirado(reserva));
    }

    private void enviar(AgendamentoResumo reserva, String texto) {
        try {
            evolution.enviarTexto(reserva.telefone(), texto);
        } catch (RuntimeException e) {
            log.error("Falha ao avisar pelo WhatsApp o agendamento {}", reserva.id(), e);
        }
    }

    @Async
    public void agendamentoPeloSite(AgendamentoResumo reserva) {
        try {
            evolution.enviarTexto(reserva.telefone(), mensagens.recebidoPeloSite(reserva));
        } catch (RuntimeException e) {
            log.error("Falha ao avisar pelo WhatsApp o agendamento {}", reserva.id(), e);
        }
    }
}
