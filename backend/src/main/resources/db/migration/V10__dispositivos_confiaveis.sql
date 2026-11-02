-- =============================================================================
-- V10 - Confirmação de dispositivo novo por e-mail
-- =============================================================================
ALTER TABLE codigos_verificacao DROP CONSTRAINT ck_codigos_finalidade;
ALTER TABLE codigos_verificacao ADD CONSTRAINT ck_codigos_finalidade
    CHECK (finalidade IN ('REDEFINIR_SENHA', 'TROCAR_SENHA', 'CONVITE', 'NOVO_DISPOSITIVO'));

-- Dispositivo confirmado: o navegador guarda um token num cookie protegido
-- (HttpOnly); aqui fica só o hash SHA-256 dele.
CREATE TABLE dispositivos_confiaveis (
    id            BIGSERIAL    PRIMARY KEY,
    usuario_id    BIGINT       NOT NULL REFERENCES usuarios_admin (id) ON DELETE CASCADE,
    token_hash    VARCHAR(64)  NOT NULL,
    nome          VARCHAR(120) NOT NULL,     -- ex.: "Chrome no Windows"
    ip            VARCHAR(64),
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_uso_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expira_em     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_dispositivos_token UNIQUE (token_hash)
);
CREATE INDEX ix_dispositivos_usuario ON dispositivos_confiaveis (usuario_id);
