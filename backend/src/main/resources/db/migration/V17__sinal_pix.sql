-- =============================================================================
-- V17 - Sinal por Pix para garantir o horário (configurado por serviço)
-- =============================================================================

-- Serviço: cobra sinal? de quanto?
ALTER TABLE servicos
    ADD COLUMN exige_sinal BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN valor_sinal NUMERIC(10,2),
    ADD CONSTRAINT ck_servicos_sinal CHECK (NOT exige_sinal OR (valor_sinal IS NOT NULL AND valor_sinal > 0));

-- Novo status: o horário fica SEGURADO enquanto a cliente paga o sinal
ALTER TABLE agendamentos DROP CONSTRAINT ck_agend_status;
ALTER TABLE agendamentos ADD CONSTRAINT ck_agend_status
    CHECK (status IN ('AGUARDANDO_SINAL','PENDENTE','CONFIRMADO','CONCLUIDO','CANCELADO','FALTOU'));

-- A barreira contra horário duplicado passa a considerar o horário segurado
ALTER TABLE agendamentos DROP CONSTRAINT ex_agendamentos_sem_conflito;
ALTER TABLE agendamentos ADD CONSTRAINT ex_agendamentos_sem_conflito
    EXCLUDE USING gist (
        profissional_id WITH =,
        tstzrange(data_inicio, data_fim, '[)') WITH &&
    )
    WHERE (status IN ('AGUARDANDO_SINAL','PENDENTE','CONFIRMADO','CONCLUIDO'));

ALTER TABLE agendamentos
    ADD COLUMN valor_sinal     NUMERIC(10,2),
    ADD COLUMN sinal_pago_em   TIMESTAMPTZ,
    ADD COLUMN sinal_expira_em TIMESTAMPTZ,
    ADD COLUMN pagamento_id    VARCHAR(80),
    ADD COLUMN pix_copia_cola  TEXT,
    ADD COLUMN codigo_publico  UUID;   -- para o site acompanhar o pagamento sem login

CREATE UNIQUE INDEX uq_agend_codigo_publico ON agendamentos (codigo_publico) WHERE codigo_publico IS NOT NULL;
CREATE INDEX ix_agend_pagamento ON agendamentos (pagamento_id) WHERE pagamento_id IS NOT NULL;
CREATE INDEX ix_agend_sinal_expira ON agendamentos (sinal_expira_em) WHERE status = 'AGUARDANDO_SINAL';

-- Salão: chave Pix (modo manual) e prazo para pagar
ALTER TABLE configuracao_salao
    ADD COLUMN chave_pix           VARCHAR(140),
    ADD COLUMN pix_nome_recebedor  VARCHAR(25),
    ADD COLUMN pix_cidade          VARCHAR(15),
    ADD COLUMN sinal_prazo_minutos INTEGER NOT NULL DEFAULT 30,
    ADD CONSTRAINT ck_config_sinal_prazo CHECK (sinal_prazo_minutos BETWEEN 10 AND 1440);
