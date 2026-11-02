-- =============================================================================
-- V5 - Identificador do salão no Google (avaliações exibidas no site)
-- =============================================================================
ALTER TABLE configuracao_salao
    ADD COLUMN google_place_id VARCHAR(200);
