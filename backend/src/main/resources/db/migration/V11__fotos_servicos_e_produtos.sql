-- =============================================================================
-- V11 - Foto de cada serviço e vitrine de produtos
-- =============================================================================
ALTER TABLE servicos
    ADD COLUMN foto_url VARCHAR(500);

CREATE TABLE produtos (
    id         BIGSERIAL     PRIMARY KEY,
    nome       VARCHAR(120)  NOT NULL,
    marca      VARCHAR(80),
    descricao  TEXT,
    preco      NUMERIC(10,2),               -- vazio = "Consulte no salão"
    foto_url   VARCHAR(500),
    disponivel BOOLEAN       NOT NULL DEFAULT TRUE,   -- false = esgotado
    ordem      INTEGER       NOT NULL DEFAULT 0,
    ativo      BOOLEAN       NOT NULL DEFAULT TRUE,   -- false = escondido do site
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_produtos_preco CHECK (preco IS NULL OR preco >= 0)
);

CREATE TRIGGER trg_produtos_updated BEFORE UPDATE ON produtos
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
