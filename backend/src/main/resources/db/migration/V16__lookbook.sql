-- =============================================================================
-- V16 - Lookbook: amostra de trabalhos feitos no salão (fotos editáveis no /admin)
-- =============================================================================
CREATE TABLE lookbook_fotos (
    id         BIGSERIAL    PRIMARY KEY,
    foto_url   VARCHAR(500) NOT NULL,
    legenda    VARCHAR(120),
    ordem      INTEGER      NOT NULL DEFAULT 0,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
