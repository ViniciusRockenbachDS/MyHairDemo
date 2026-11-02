-- =============================================================================
-- V13 - Perguntas frequentes que o robô do WhatsApp responde (sem IA)
-- =============================================================================
CREATE TABLE perguntas_frequentes (
    id             BIGSERIAL    PRIMARY KEY,
    pergunta       VARCHAR(200) NOT NULL,   -- como a cliente costuma perguntar (referência)
    palavras_chave TEXT         NOT NULL,   -- separadas por vírgula
    resposta       TEXT         NOT NULL,
    ordem          INTEGER      NOT NULL DEFAULT 0,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
