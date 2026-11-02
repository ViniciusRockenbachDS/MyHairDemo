package br.com.jofashionhair.exception;

import java.time.LocalDateTime;

import br.com.jofashionhair.domain.Disponibilidade;

/**
 * O horário pedido não pode ser reservado (HTTP 409).
 * Carrega o contexto do pedido para que o handler (site) ou a máquina de
 * estados (WhatsApp) possam sugerir horários alternativos.
 */
public class HorarioIndisponivelException extends RuntimeException {

    private final Disponibilidade motivo;
    private final Long servicoId;
    private final Long profissionalId;   // null = "sem preferência"
    private final LocalDateTime inicio;  // horário local do salão

    public HorarioIndisponivelException(Disponibilidade motivo, Long servicoId,
                                        Long profissionalId, LocalDateTime inicio) {
        super(motivo.getMensagem());
        this.motivo = motivo;
        this.servicoId = servicoId;
        this.profissionalId = profissionalId;
        this.inicio = inicio;
    }

    public Disponibilidade getMotivo() { return motivo; }
    public Long getServicoId() { return servicoId; }
    public Long getProfissionalId() { return profissionalId; }
    public LocalDateTime getInicio() { return inicio; }
}
