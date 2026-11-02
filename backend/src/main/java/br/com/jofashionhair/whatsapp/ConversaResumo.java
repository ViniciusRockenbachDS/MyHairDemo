package br.com.jofashionhair.whatsapp;

import java.time.LocalDateTime;

import br.com.jofashionhair.domain.EstadoConversa;

/** Linha da lista de conversas do /admin. */
public record ConversaResumo(String telefone, EstadoConversa estado,
                             LocalDateTime ultimaInteracao, LocalDateTime expiraEm, String ultimaMensagem) {
}
