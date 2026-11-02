package br.com.jofashionhair.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.jofashionhair.domain.Agendamento;
import br.com.jofashionhair.domain.StatusAgendamento;

/** Visão do agendamento devolvida pela API (horários já no fuso do salão). */
public record AgendamentoResumo(
        Long id,
        String cliente,
        String telefone,
        Long profissionalId,
        String profissional,
        Long servicoId,
        String servico,
        LocalDateTime inicio,
        LocalDateTime fim,
        Integer duracaoMinutos,
        BigDecimal preco,
        StatusAgendamento status,
        BigDecimal valorSinal,
        boolean sinalPago) {

    /** Deve ser chamado DENTRO da transação (as associações são LAZY). */
    public static AgendamentoResumo de(Agendamento a, RelogioSalao relogio) {
        return new AgendamentoResumo(
                a.getId(),
                a.getCliente().getNome(),
                a.getCliente().getTelefone(),
                a.getProfissional().getId(),
                a.getProfissional().getNome(),
                a.getServico().getId(),
                a.getServico().getNome(),
                relogio.paraLocal(a.getDataInicio()),
                relogio.paraLocal(a.getDataFim()),
                a.getDuracaoMinutos(),
                a.getPrecoCobrado(),
                a.getStatus(),
                a.getValorSinal(),
                a.getSinalPagoEm() != null);
    }
}
