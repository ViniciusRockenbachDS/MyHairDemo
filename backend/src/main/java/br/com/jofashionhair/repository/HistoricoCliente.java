package br.com.jofashionhair.repository;

import java.time.OffsetDateTime;

/** Linha da lista de clientes do /admin com os contadores de no-show. */
public record HistoricoCliente(
        Long clienteId,
        String nome,
        String telefone,
        Long totalAgendamentos,
        Long totalConcluidos,
        Long totalCancelados,
        Long totalFaltas,
        OffsetDateTime ultimaFaltaEm) {
}
