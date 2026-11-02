-- =============================================================================
-- V4 - Fotos e biografias exibidas no site (editáveis no /admin)
-- =============================================================================
ALTER TABLE profissionais
    ADD COLUMN bio      TEXT,
    ADD COLUMN foto_url VARCHAR(500);

ALTER TABLE configuracao_salao
    ADD COLUMN logo_url        VARCHAR(500),
    ADD COLUMN foto_espaco_url VARCHAR(500),
    ADD COLUMN foto_visita_url VARCHAR(500);
