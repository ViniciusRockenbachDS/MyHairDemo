-- =============================================================================
-- V15 - Promoções (desconto em dias/serviços) e campanhas por WhatsApp
-- =============================================================================
CREATE TABLE promocoes (
    id                  BIGSERIAL    PRIMARY KEY,
    titulo              VARCHAR(120) NOT NULL,
    descricao           TEXT,
    desconto_percentual SMALLINT     NOT NULL,
    dias_semana         VARCHAR(20)  NOT NULL DEFAULT '',   -- "2,4" = terça e quinta; vazio = todos os dias
    data_inicio         DATE,
    data_fim            DATE,
    todos_servicos      BOOLEAN      NOT NULL DEFAULT TRUE,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_promocoes_desconto CHECK (desconto_percentual BETWEEN 1 AND 90),
    CONSTRAINT ck_promocoes_periodo  CHECK (data_inicio IS NULL OR data_fim IS NULL OR data_fim >= data_inicio)
);

CREATE TABLE promocao_servicos (
    promocao_id BIGINT NOT NULL REFERENCES promocoes (id) ON DELETE CASCADE,
    servico_id  BIGINT NOT NULL REFERENCES servicos (id)  ON DELETE CASCADE,
    PRIMARY KEY (promocao_id, servico_id)
);

-- Histórico dos envios por WhatsApp
CREATE TABLE campanhas_whatsapp (
    id           BIGSERIAL    PRIMARY KEY,
    promocao_id  BIGINT       REFERENCES promocoes (id) ON DELETE SET NULL,
    titulo       VARCHAR(120) NOT NULL,
    mensagem     TEXT         NOT NULL,
    publico      VARCHAR(40)  NOT NULL,
    total        INTEGER      NOT NULL DEFAULT 0,
    enviados     INTEGER      NOT NULL DEFAULT 0,
    falhas       INTEGER      NOT NULL DEFAULT 0,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ENVIANDO',
    criado_em    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    concluido_em TIMESTAMPTZ,
    CONSTRAINT ck_campanhas_status CHECK (status IN ('ENVIANDO', 'CONCLUIDA'))
);

-- Preferência da cliente: true = autorizou promoções, false = pediu para sair, null = não informou
ALTER TABLE clientes ADD COLUMN aceita_promocoes BOOLEAN;
