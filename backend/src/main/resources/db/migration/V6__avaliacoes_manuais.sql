-- =============================================================================
-- V6 - Avaliações cadastradas pelo dono no /admin (substitui a busca no Google)
-- =============================================================================
ALTER TABLE configuracao_salao DROP COLUMN IF EXISTS google_place_id;

-- Resumo exibido no topo da seção (ex.: 4,9 · 78 avaliações)
ALTER TABLE configuracao_salao
    ADD COLUMN avaliacoes_nota  NUMERIC(2,1),
    ADD COLUMN avaliacoes_total INTEGER,
    ADD COLUMN avaliacoes_link  VARCHAR(500),
    ADD CONSTRAINT ck_config_avaliacoes_nota  CHECK (avaliacoes_nota IS NULL OR avaliacoes_nota BETWEEN 1 AND 5),
    ADD CONSTRAINT ck_config_avaliacoes_total CHECK (avaliacoes_total IS NULL OR avaliacoes_total >= 0);

CREATE TABLE avaliacoes (
    id         BIGSERIAL    PRIMARY KEY,
    autor      VARCHAR(120) NOT NULL,
    texto      TEXT         NOT NULL,
    nota       SMALLINT     NOT NULL DEFAULT 5,
    data       DATE,
    foto_url   VARCHAR(500),
    link       VARCHAR(500),
    ordem      INTEGER      NOT NULL DEFAULT 0,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_avaliacoes_nota CHECK (nota BETWEEN 1 AND 5)
);
