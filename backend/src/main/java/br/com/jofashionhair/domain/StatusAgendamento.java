package br.com.jofashionhair.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida do agendamento.
 *
 * <pre>
 * AGUARDANDO_SINAL ──► CONFIRMADO (sinal pago) | CANCELADO (prazo acabou)
 * PENDENTE ──► CONFIRMADO ──► CONCLUIDO
 *    │             │
 *    ├─► CANCELADO ◄┤
 *    └─► FALTOU   ◄─┘
 * </pre>
 */
public enum StatusAgendamento {
    /** Horário segurado enquanto a cliente paga o sinal por Pix. */
    AGUARDANDO_SINAL,
    PENDENTE,
    CONFIRMADO,
    CONCLUIDO,
    CANCELADO,
    FALTOU;

    /** Status que ocupam a agenda (deve espelhar o WHERE da constraint no banco). */
    public static final Set<StatusAgendamento> OCUPAM_AGENDA = EnumSet.of(AGUARDANDO_SINAL, PENDENTE, CONFIRMADO, CONCLUIDO);

    public boolean podeIrPara(StatusAgendamento destino) {
        return switch (this) {
            case AGUARDANDO_SINAL -> destino == CONFIRMADO || destino == CANCELADO;
            case PENDENTE -> destino == CONFIRMADO || destino == CONCLUIDO
                    || destino == CANCELADO || destino == FALTOU;
            case CONFIRMADO -> destino == CONCLUIDO || destino == CANCELADO || destino == FALTOU;
            case CONCLUIDO, CANCELADO, FALTOU -> false;
        };
    }
}
