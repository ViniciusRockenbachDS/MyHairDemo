package br.com.jofashionhair.service;

import java.time.LocalDateTime;

import br.com.jofashionhair.domain.OrigemAgendamento;

/**
 * Pedido de reserva, igual para os três canais (site, WhatsApp e /admin).
 *
 * @param profissionalId null = "sem preferência" (o sistema escolhe uma livre)
 * @param inicio         horário LOCAL do salão
 */
public record SolicitacaoAgendamento(
        Long servicoId,
        Long profissionalId,
        LocalDateTime inicio,
        String nomeCliente,
        String telefone,
        OrigemAgendamento origem,
        String observacoes) {
}
