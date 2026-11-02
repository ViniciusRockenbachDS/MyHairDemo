-- =============================================================================
-- SEED DE DESENVOLVIMENTO 2 (perfil "dev"): mais serviços e profissionais
-- de EXEMPLO para testar o site. Nomes, preços e textos são fictícios.
-- =============================================================================

-- Profissionais de exemplo (renomeia as do primeiro seed)
UPDATE profissionais SET nome = 'Ana Paula', especialidade = 'Cabeleireira',
       bio = 'Especialista em cortes femininos e coloração, com foco em resultados naturais. (perfil de exemplo)'
 WHERE nome = 'Profissional A';
UPDATE profissionais SET nome = 'Bruna', especialidade = 'Nail designer',
       bio = 'Unhas em gel, alongamentos e nail art com acabamento impecável. (perfil de exemplo)'
 WHERE nome = 'Profissional B';
UPDATE profissionais SET nome = 'Carla', especialidade = 'Esteticista',
       bio = 'Design de sobrancelhas, maquiagem e cuidados com a pele. (perfil de exemplo)'
 WHERE nome = 'Profissional C';

INSERT INTO profissionais (nome, especialidade, bio)
SELECT 'Daniela', 'Cabeleireira', 'Escovas, progressivas e tratamentos capilares. (perfil de exemplo)'
 WHERE NOT EXISTS (SELECT 1 FROM profissionais WHERE nome = 'Daniela');

-- Mais serviços (só insere os que ainda não existem)
INSERT INTO servicos (nome, descricao, categoria, preco, duracao_minutos, ordem_exibicao)
SELECT v.nome, v.descricao, v.categoria, v.preco, v.duracao, v.ordem
  FROM (VALUES
        ('Coloração',           'Coloração completa com produtos profissionais', 'CABELO',   180.00, 120,  7),
        ('Mechas',              'Mechas ou luzes com tonalização',                'CABELO',   350.00, 180,  8),
        ('Escova Progressiva',  'Alisamento com redução de volume',               'CABELO',   280.00, 150,  9),
        ('Pedicure',            'Cutilagem e esmaltação dos pés',                 'UNHAS',     45.00,  45, 10),
        ('Manicure e Pedicure', 'Mãos e pés no mesmo horário',                    'UNHAS',     80.00,  90, 11),
        ('Maquiagem',           'Maquiagem para festas e eventos',                'ESTETICA', 150.00,  60, 12),
        ('Limpeza de Pele',     'Limpeza profunda com extração e máscara',        'ESTETICA', 130.00,  75, 13)
       ) AS v(nome, descricao, categoria, preco, duracao, ordem)
 WHERE NOT EXISTS (SELECT 1 FROM servicos s WHERE lower(s.nome) = lower(v.nome));

-- O que cada uma faz
INSERT INTO profissional_servicos (profissional_id, servico_id)
SELECT p.id, s.id
  FROM profissionais p
  JOIN servicos s ON (p.nome, s.nome) IN (
        ('Ana Paula', 'Corte Feminino'), ('Ana Paula', 'Escova'), ('Ana Paula', 'Hidratação'),
        ('Ana Paula', 'Coloração'), ('Ana Paula', 'Mechas'),
        ('Daniela', 'Escova'), ('Daniela', 'Escova Progressiva'), ('Daniela', 'Hidratação'),
        ('Daniela', 'Corte Feminino'),
        ('Bruna', 'Unha em Gel'), ('Bruna', 'Manicure'), ('Bruna', 'Pedicure'), ('Bruna', 'Manicure e Pedicure'),
        ('Carla', 'Design de Sobrancelha'), ('Carla', 'Maquiagem'), ('Carla', 'Limpeza de Pele'),
        ('Carla', 'Manicure'), ('Carla', 'Pedicure')
       )
ON CONFLICT DO NOTHING;

-- Expediente da Daniela: terça a sábado
INSERT INTO horario_trabalho (profissional_id, dia_semana, hora_inicio, hora_fim)
SELECT p.id, d, f.ini, f.fim
  FROM profissionais p
 CROSS JOIN generate_series(2, 5) AS d
 CROSS JOIN (VALUES ('09:00'::time, '12:00'::time), ('13:00'::time, '19:00'::time)) AS f(ini, fim)
 WHERE p.nome = 'Daniela'
ON CONFLICT DO NOTHING;

INSERT INTO horario_trabalho (profissional_id, dia_semana, hora_inicio, hora_fim)
SELECT p.id, 6, '08:00'::time, '14:00'::time
  FROM profissionais p
 WHERE p.nome = 'Daniela'
ON CONFLICT DO NOTHING;

-- Dados do salão para o site ficar completo nos testes (troque no /admin)
UPDATE configuracao_salao
   SET slogan    = 'Beleza com hora marcada, atendimento exclusivo.',
       descricao = 'Cabelo, unhas e estética em um só lugar, com profissionais que conhecem o seu estilo.' || chr(10) || chr(10)
                   || 'Você agenda pelo celular em um minuto e, na hora marcada, a sua cadeira já está esperando.',
       endereco  = COALESCE(endereco, 'Endereço de exemplo, 100 - Centro')
 WHERE id = 1;
