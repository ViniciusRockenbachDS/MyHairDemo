-- =============================================================================
-- SEED DE DESENVOLVIMENTO 3 (perfil "dev"): produtos de EXEMPLO
-- =============================================================================
INSERT INTO produtos (nome, marca, descricao, preco, ordem)
SELECT v.nome, v.marca, v.descricao, v.preco, v.ordem
  FROM (VALUES
        ('Shampoo Hidratante 300 ml', 'Marca exemplo', 'Limpeza suave para cabelos secos. (produto de exemplo)', 59.90, 1),
        ('Máscara de Reconstrução 250 g', 'Marca exemplo', 'Tratamento intensivo para fios danificados. (produto de exemplo)', 89.90, 2),
        ('Óleo Finalizador 60 ml', 'Marca exemplo', 'Brilho e controle do frizz. (produto de exemplo)', 49.90, 3),
        ('Kit Esmaltes Tendência', 'Marca exemplo', 'Cores da estação para manter as unhas em dia. (produto de exemplo)', NULL, 4)
       ) AS v(nome, marca, descricao, preco, ordem)
 WHERE NOT EXISTS (SELECT 1 FROM produtos p WHERE p.nome = v.nome);
