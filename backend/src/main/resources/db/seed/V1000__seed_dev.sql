-- =============================================================================
-- SEED DE DESENVOLVIMENTO  (carregado apenas com o perfil "dev")
-- Nomes, preços e durações abaixo são EXEMPLOS: ajuste pelo /admin depois.
-- =============================================================================

INSERT INTO profissionais (nome, especialidade) VALUES
    ('Profissional A', 'Cabelo'),
    ('Profissional B', 'Unhas'),
    ('Profissional C', 'Estética e Unhas');

INSERT INTO servicos (nome, descricao, categoria, preco, duracao_minutos, ordem_exibicao) VALUES
    ('Escova',                'Escova modelada',                    'CABELO',   60.00, 40, 1),
    ('Corte Feminino',        'Corte com finalização',              'CABELO',   80.00, 50, 2),
    ('Hidratação',            'Hidratação profunda dos fios',       'CABELO',   90.00, 45, 3),
    ('Unha em Gel',           'Alongamento/esmaltação em gel',      'UNHAS',   120.00, 60, 4),
    ('Manicure',              'Cutilagem e esmaltação',             'UNHAS',    40.00, 45, 5),
    ('Design de Sobrancelha', 'Design com técnica de visagismo',    'ESTETICA', 45.00, 30, 6);

-- Vínculos profissional x serviço (N:N)
INSERT INTO profissional_servicos (profissional_id, servico_id)
SELECT p.id, s.id
FROM profissionais p
JOIN servicos s
  ON (p.nome, s.nome) IN (
        ('Profissional A', 'Escova'),
        ('Profissional A', 'Corte Feminino'),
        ('Profissional A', 'Hidratação'),
        ('Profissional B', 'Unha em Gel'),
        ('Profissional B', 'Manicure'),
        ('Profissional C', 'Manicure'),
        ('Profissional C', 'Design de Sobrancelha'),
        ('Profissional C', 'Escova')
     );

-- Segunda a sexta: 09:00-12:00 e 13:30-19:00 (com intervalo de almoço)
INSERT INTO horario_trabalho (profissional_id, dia_semana, hora_inicio, hora_fim)
SELECT p.id, d, f.ini, f.fim
FROM profissionais p
CROSS JOIN generate_series(1, 5) AS d
CROSS JOIN (VALUES ('09:00'::time, '12:00'::time),
                   ('13:30'::time, '19:00'::time)) AS f(ini, fim);

-- Sábado: 08:00-14:00
INSERT INTO horario_trabalho (profissional_id, dia_semana, hora_inicio, hora_fim)
SELECT p.id, 6, '08:00'::time, '14:00'::time
FROM profissionais p;

-- Cliente de teste
INSERT INTO clientes (nome, telefone) VALUES ('Cliente Teste', '5547999990000');
