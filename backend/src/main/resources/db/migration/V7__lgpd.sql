-- =============================================================================
-- V7 - LGPD: dados do responsável, política de privacidade e anonimização
-- =============================================================================
ALTER TABLE configuracao_salao
    ADD COLUMN razao_social         VARCHAR(200),
    ADD COLUMN cnpj                 VARCHAR(20),
    ADD COLUMN email_privacidade    VARCHAR(160),
    ADD COLUMN politica_privacidade TEXT,          -- vazio = o site usa o modelo padrão
    ADD COLUMN retencao_meses       INTEGER NOT NULL DEFAULT 24,
    ADD CONSTRAINT ck_config_retencao CHECK (retencao_meses BETWEEN 6 AND 120);

-- Cliente anonimizada: nome genérico e SEM telefone (os agendamentos continuam
-- nos relatórios, sem identificar a pessoa).
ALTER TABLE clientes
    ALTER COLUMN telefone DROP NOT NULL,
    ADD COLUMN anonimizado_em TIMESTAMPTZ;
