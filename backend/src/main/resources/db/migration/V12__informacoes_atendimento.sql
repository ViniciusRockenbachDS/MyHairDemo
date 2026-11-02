-- =============================================================================
-- V12 - Informações extras para o atendimento automático do WhatsApp
-- (formas de pagamento, estacionamento, política de atraso, etc.)
-- =============================================================================
ALTER TABLE configuracao_salao
    ADD COLUMN info_atendimento TEXT;
