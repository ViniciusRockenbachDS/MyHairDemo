-- =============================================================================
-- V3 - Informações do salão exibidas no site e usadas pelo robô
-- Tabela de UMA linha (id = 1), editada pelo /admin.
-- =============================================================================
CREATE TABLE configuracao_salao (
    id          SMALLINT     PRIMARY KEY DEFAULT 1,
    nome        VARCHAR(120) NOT NULL,
    slogan      VARCHAR(200),
    descricao   TEXT,
    endereco    VARCHAR(250),
    cidade      VARCHAR(120),
    telefone    VARCHAR(20),
    whatsapp    VARCHAR(20),
    instagram   VARCHAR(60),
    link_mapa   VARCHAR(500),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_config_salao_unica CHECK (id = 1)
);

-- Dados conhecidos; endereço, telefones e textos o dono preenche no /admin.
INSERT INTO configuracao_salao (id, nome, cidade, instagram)
VALUES (1, 'Jo Fashion Hair', 'Campo Alegre - SC', 'jofashionhair_sc');

CREATE TRIGGER trg_config_salao_updated BEFORE UPDATE ON configuracao_salao
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
