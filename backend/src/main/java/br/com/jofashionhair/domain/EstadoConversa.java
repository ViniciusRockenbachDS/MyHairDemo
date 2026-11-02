package br.com.jofashionhair.domain;

/**
 * Estados da conversa no WhatsApp.
 *
 * <pre>
 * INICIAL ─► AGUARDANDO_SERVICO ─► AGUARDANDO_PROFISSIONAL ─► AGUARDANDO_DATA_HORA
 *                                                                   │
 *                         AGUARDANDO_ESCOLHA_HORARIO ◄── ocupado ───┤
 *                                   │                               │ livre
 *                                   └──────────────► FINALIZADO ◄───┘
 *
 * Paralelos: AGUARDANDO_ESCOLHA_CANCELAMENTO, AGUARDANDO_CONFIRMACAO_LEMBRETE (Passo 5)
 *            ATENDIMENTO_HUMANO (robô em silêncio até expirar ou o cliente mandar "menu")
 * </pre>
 *
 * O robô não exige uma ordem rígida: se o cliente já disser tudo numa frase
 * ("unha em gel sexta às 17h com a Ana"), as etapas já respondidas são puladas.
 */
public enum EstadoConversa {
    INICIAL,
    AGUARDANDO_SERVICO,
    AGUARDANDO_PROFISSIONAL,
    AGUARDANDO_DATA_HORA,
    AGUARDANDO_ESCOLHA_HORARIO,
    AGUARDANDO_ESCOLHA_CANCELAMENTO,
    AGUARDANDO_CONFIRMACAO_LEMBRETE,
    ATENDIMENTO_HUMANO,
    FINALIZADO
}
