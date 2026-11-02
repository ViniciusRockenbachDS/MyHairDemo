-- =============================================================================
-- V9 - Logins individuais do painel e códigos de verificação por e-mail
-- =============================================================================
CREATE TABLE usuarios_admin (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,       -- BCrypt (nunca a senha em si)
    papel       VARCHAR(20)  NOT NULL DEFAULT 'EQUIPE',
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuarios_papel CHECK (papel IN ('DONO', 'EQUIPE'))
);
CREATE UNIQUE INDEX uq_usuarios_admin_email ON usuarios_admin (lower(email));

-- Código de 6 dígitos enviado por e-mail (guardado só como hash SHA-256)
CREATE TABLE codigos_verificacao (
    id          BIGSERIAL   PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuarios_admin (id) ON DELETE CASCADE,
    finalidade  VARCHAR(30) NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    expira_em   TIMESTAMPTZ NOT NULL,
    tentativas  INTEGER     NOT NULL DEFAULT 0,
    usado_em    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_codigos_finalidade CHECK (finalidade IN ('REDEFINIR_SENHA', 'TROCAR_SENHA', 'CONVITE'))
);
CREATE INDEX ix_codigos_usuario ON codigos_verificacao (usuario_id, finalidade);
