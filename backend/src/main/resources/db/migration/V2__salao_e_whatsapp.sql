-- =============================================================================
-- V2 - Horário de funcionamento do salão + idempotência do webhook do WhatsApp
-- =============================================================================

-- -----------------------------------------------------------------------------
-- HORÁRIO DE FUNCIONAMENTO DO SALÃO (um registro por dia da semana)
-- O horário disponível para agendar = expediente da profissional
--                                    ∩ funcionamento do salão.
-- dia_semana: 1 = segunda ... 7 = domingo
-- -----------------------------------------------------------------------------
CREATE TABLE horario_funcionamento (
    dia_semana      SMALLINT PRIMARY KEY,
    fechado         BOOLEAN  NOT NULL DEFAULT FALSE,
    hora_abertura   TIME,
    hora_fechamento TIME,
    CONSTRAINT ck_func_dia   CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_func_horas CHECK (
        fechado
        OR (hora_abertura IS NOT NULL
            AND hora_fechamento IS NOT NULL
            AND hora_fechamento > hora_abertura)
    )
);

-- Valores INICIAIS: o dono ajusta pelo /admin.
INSERT INTO horario_funcionamento (dia_semana, fechado, hora_abertura, hora_fechamento) VALUES
    (1, FALSE, '08:00', '20:00'),
    (2, FALSE, '08:00', '20:00'),
    (3, FALSE, '08:00', '20:00'),
    (4, FALSE, '08:00', '20:00'),
    (5, FALSE, '08:00', '20:00'),
    (6, FALSE, '08:00', '18:00'),
    (7, TRUE,  NULL,    NULL);

-- -----------------------------------------------------------------------------
-- MENSAGENS DO WEBHOOK JÁ PROCESSADAS
-- A Evolution API pode reenviar o mesmo evento; este registro garante que cada
-- mensagem seja respondida uma única vez. Limpeza automática após 7 dias.
-- -----------------------------------------------------------------------------
CREATE TABLE whatsapp_mensagens_processadas (
    message_id  VARCHAR(100) PRIMARY KEY,
    recebida_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_wpp_msg_recebida ON whatsapp_mensagens_processadas (recebida_em);
