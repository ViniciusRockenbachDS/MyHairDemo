package br.com.jofashionhair.service;

import java.time.LocalDateTime;

/** Horário alternativo oferecido quando o pedido não está livre. */
public record SugestaoHorario(Long profissionalId, String profissionalNome, LocalDateTime inicio) {
}
