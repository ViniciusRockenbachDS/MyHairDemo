-- =============================================================================
-- Jo Fashion Hair - Schema inicial (PostgreSQL 14+)
--
-- Decisões de projeto importantes:
--  1. Todos os instantes usam TIMESTAMPTZ (o fuso do salão é America/Sao_Paulo,
--     convertido na aplicação).
--  2. O agendamento guarda um SNAPSHOT da duração e do preço no momento da
--     reserva. Assim, quando o admin altera preço/duração de um serviço, as
--     reservas já feitas NÃO mudam; as NOVAS consultas usam o valor atualizado.
--  3. O double booking é impedido pelo próprio banco (EXCLUDE USING gist).
--     Mesmo que duas requisições (site + WhatsApp) cheguem juntas, só uma passa.
--  4. Serviços/profissionais NÃO são apagados fisicamente: usa-se `ativo = false`
--     (as FKs de agendamentos são RESTRICT para preservar o histórico).
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- -----------------------------------------------------------------------------
-- Função genérica para manter updated_at
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -----------------------------------------------------------------------------
-- CLIENTES
-- telefone: somente dígitos com DDI, ex.: 5547999998888 (formato do WhatsApp)
-- -----------------------------------------------------------------------------
CREATE TABLE clientes (
    id          BIGSERIAL    PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    telefone    VARCHAR(20)  NOT NULL,
    email       VARCHAR(160),
    observacoes TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_clientes_telefone UNIQUE (telefone),
    CONSTRAINT ck_clientes_telefone CHECK (telefone ~ '^[0-9]{10,15}$')
);

-- -----------------------------------------------------------------------------
-- PROFISSIONAIS
-- -----------------------------------------------------------------------------
CREATE TABLE profissionais (
    id            BIGSERIAL    PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    especialidade VARCHAR(120),
    telefone      VARCHAR(20),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -----------------------------------------------------------------------------
-- SERVIÇOS (preço e duração editáveis pelo /admin a qualquer momento)
-- -----------------------------------------------------------------------------
CREATE TABLE servicos (
    id               BIGSERIAL     PRIMARY KEY,
    nome             VARCHAR(120)  NOT NULL,
    descricao        TEXT,
    categoria        VARCHAR(40)   NOT NULL DEFAULT 'OUTROS',  -- CABELO, UNHAS, ESTETICA, OUTROS
    preco            NUMERIC(10,2) NOT NULL,
    duracao_minutos  INTEGER       NOT NULL,
    ordem_exibicao   INTEGER       NOT NULL DEFAULT 0,
    ativo            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_servicos_preco   CHECK (preco >= 0),
    CONSTRAINT ck_servicos_duracao CHECK (duracao_minutos BETWEEN 5 AND 720)
);
CREATE UNIQUE INDEX uq_servicos_nome ON servicos (lower(nome));

-- -----------------------------------------------------------------------------
-- RELACIONAMENTO N:N PROFISSIONAL x SERVIÇO
-- -----------------------------------------------------------------------------
CREATE TABLE profissional_servicos (
    profissional_id BIGINT      NOT NULL REFERENCES profissionais (id) ON DELETE CASCADE,
    servico_id      BIGINT      NOT NULL REFERENCES servicos (id)      ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (profissional_id, servico_id)
);
CREATE INDEX ix_prof_servicos_servico ON profissional_servicos (servico_id);

-- -----------------------------------------------------------------------------
-- HORÁRIO DE TRABALHO (grade semanal por profissional)
-- dia_semana segue ISO-8601 / java.time.DayOfWeek: 1 = segunda ... 7 = domingo
-- Várias faixas por dia permitem intervalo de almoço (ex.: 09-12 e 13-19).
-- A sobreposição entre faixas do mesmo dia é validada na camada de serviço.
-- -----------------------------------------------------------------------------
CREATE TABLE horario_trabalho (
    id              BIGSERIAL   PRIMARY KEY,
    profissional_id BIGINT      NOT NULL REFERENCES profissionais (id) ON DELETE CASCADE,
    dia_semana      SMALLINT    NOT NULL,
    hora_inicio     TIME        NOT NULL,
    hora_fim        TIME        NOT NULL,
    CONSTRAINT ck_horario_dia   CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_faixa CHECK (hora_fim > hora_inicio),
    CONSTRAINT uq_horario_inicio UNIQUE (profissional_id, dia_semana, hora_inicio)
);

-- -----------------------------------------------------------------------------
-- BLOQUEIOS MANUAIS DE AGENDA (folga, médico, curso, etc.)
-- -----------------------------------------------------------------------------
CREATE TABLE bloqueios_agenda (
    id              BIGSERIAL   PRIMARY KEY,
    profissional_id BIGINT      NOT NULL REFERENCES profissionais (id) ON DELETE CASCADE,
    data_inicio     TIMESTAMPTZ NOT NULL,
    data_fim        TIMESTAMPTZ NOT NULL,
    motivo          VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_bloqueio_periodo CHECK (data_fim > data_inicio)
);
CREATE INDEX ix_bloqueios_prof_periodo ON bloqueios_agenda (profissional_id, data_inicio, data_fim);

-- -----------------------------------------------------------------------------
-- AGENDAMENTOS
-- Ciclo de vida: PENDENTE -> CONFIRMADO -> CONCLUIDO | CANCELADO | FALTOU
-- Ocupam a agenda: PENDENTE, CONFIRMADO, CONCLUIDO (CANCELADO e FALTOU liberam).
-- -----------------------------------------------------------------------------
CREATE TABLE agendamentos (
    id                        BIGSERIAL     PRIMARY KEY,
    cliente_id                BIGINT        NOT NULL REFERENCES clientes (id)      ON DELETE RESTRICT,
    profissional_id           BIGINT        NOT NULL REFERENCES profissionais (id) ON DELETE RESTRICT,
    servico_id                BIGINT        NOT NULL REFERENCES servicos (id)      ON DELETE RESTRICT,

    data_inicio               TIMESTAMPTZ   NOT NULL,
    data_fim                  TIMESTAMPTZ   NOT NULL,

    -- Snapshots: valores vigentes NO MOMENTO da reserva
    duracao_minutos           INTEGER       NOT NULL,
    preco_cobrado             NUMERIC(10,2) NOT NULL,

    status                    VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    origem                    VARCHAR(20)   NOT NULL DEFAULT 'SITE',
    observacoes               TEXT,

    -- Controle dos lembretes automáticos (idempotência do @Scheduled)
    lembrete_24h_enviado_em   TIMESTAMPTZ,
    lembrete_2h_enviado_em    TIMESTAMPTZ,
    confirmado_cliente_em     TIMESTAMPTZ,

    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT ck_agend_status  CHECK (status IN ('PENDENTE','CONFIRMADO','CONCLUIDO','CANCELADO','FALTOU')),
    CONSTRAINT ck_agend_origem  CHECK (origem IN ('SITE','WHATSAPP','ADMIN')),
    CONSTRAINT ck_agend_duracao CHECK (duracao_minutos > 0),
    CONSTRAINT ck_agend_preco   CHECK (preco_cobrado >= 0),
    -- garante que data_fim sempre = início + duração (integridade do snapshot)
    CONSTRAINT ck_agend_fim     CHECK (data_fim = data_inicio + (duracao_minutos * INTERVAL '1 minute')),

    -- >>> BARREIRA ATÔMICA CONTRA DOUBLE BOOKING <<<
    -- Uma profissional não pode ter dois agendamentos "ativos" com intervalos
    -- sobrepostos. O intervalo é [início, fim): um serviço que termina às 18:00
    -- NÃO conflita com outro que começa às 18:00.
    CONSTRAINT ex_agendamentos_sem_conflito
        EXCLUDE USING gist (
            profissional_id WITH =,
            tstzrange(data_inicio, data_fim, '[)') WITH &&
        )
        WHERE (status IN ('PENDENTE','CONFIRMADO','CONCLUIDO'))
);

CREATE INDEX ix_agend_prof_inicio   ON agendamentos (profissional_id, data_inicio);
CREATE INDEX ix_agend_cliente       ON agendamentos (cliente_id, data_inicio DESC);
CREATE INDEX ix_agend_status_inicio ON agendamentos (status, data_inicio);
-- Índice parcial usado pela rotina de lembretes
CREATE INDEX ix_agend_lembretes     ON agendamentos (data_inicio)
    WHERE status IN ('PENDENTE','CONFIRMADO');

-- -----------------------------------------------------------------------------
-- ESTADO DA CONVERSA NO WHATSAPP (State Machine persistida)
-- estado: INICIAL, AGUARDANDO_SERVICO, AGUARDANDO_PROFISSIONAL,
--         AGUARDANDO_ESCOLHA_ALTERNATIVA, AGUARDANDO_CONFIRMACAO_LEMBRETE, ...
-- (sem CHECK de propósito: o enum Java evolui sem exigir nova migration)
-- contexto: dados já coletados, ex.:
--   {"data":"2026-10-16","hora":"17:00","servicoId":3,"profissionalId":null}
-- -----------------------------------------------------------------------------
CREATE TABLE atendimento_whatsapp_estado (
    id                  BIGSERIAL    PRIMARY KEY,
    telefone            VARCHAR(20)  NOT NULL,
    cliente_id          BIGINT       REFERENCES clientes (id) ON DELETE SET NULL,
    estado              VARCHAR(50)  NOT NULL DEFAULT 'INICIAL',
    contexto            JSONB        NOT NULL DEFAULT '{}'::jsonb,
    ultimo_message_id   VARCHAR(100),                       -- idempotência do webhook
    ultima_interacao_em TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expira_em           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_wpp_estado_telefone UNIQUE (telefone)
);
CREATE INDEX ix_wpp_estado_expira ON atendimento_whatsapp_estado (expira_em);

-- -----------------------------------------------------------------------------
-- HISTÓRICO DO CLIENTE (alimenta o controle de no-show no /admin)
-- -----------------------------------------------------------------------------
CREATE VIEW vw_historico_cliente AS
SELECT
    c.id                                                    AS cliente_id,
    c.nome,
    c.telefone,
    COUNT(a.id)                                             AS total_agendamentos,
    COUNT(a.id) FILTER (WHERE a.status = 'CONCLUIDO')       AS total_concluidos,
    COUNT(a.id) FILTER (WHERE a.status = 'CANCELADO')       AS total_cancelados,
    COUNT(a.id) FILTER (WHERE a.status = 'FALTOU')          AS total_faltas,
    MAX(a.data_inicio) FILTER (WHERE a.status = 'FALTOU')   AS ultima_falta_em
FROM clientes c
LEFT JOIN agendamentos a ON a.cliente_id = c.id
GROUP BY c.id, c.nome, c.telefone;

-- -----------------------------------------------------------------------------
-- TRIGGERS de updated_at
-- -----------------------------------------------------------------------------
CREATE TRIGGER trg_clientes_updated      BEFORE UPDATE ON clientes
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_profissionais_updated BEFORE UPDATE ON profissionais
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_servicos_updated      BEFORE UPDATE ON servicos
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_agendamentos_updated  BEFORE UPDATE ON agendamentos
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_wpp_estado_updated    BEFORE UPDATE ON atendimento_whatsapp_estado
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
