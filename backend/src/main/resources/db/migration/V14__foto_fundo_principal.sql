-- =============================================================================
-- V14 - Foto de fundo da tela principal do site (editável no /admin)
-- =============================================================================
ALTER TABLE configuracao_salao
    ADD COLUMN foto_hero_url VARCHAR(500);
