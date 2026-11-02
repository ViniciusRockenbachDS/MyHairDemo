-- =============================================================================
-- SEED DE DESENVOLVIMENTO 4 (perfil "dev"): perguntas frequentes de EXEMPLO
-- Ajuste as respostas para as regras reais do salão.
-- =============================================================================
INSERT INTO perguntas_frequentes (pergunta, palavras_chave, resposta, ordem)
SELECT v.pergunta, v.palavras, v.resposta, v.ordem
  FROM (VALUES
        ('Quais formas de pagamento?', 'pix, cartao, credito, debito, dinheiro, pagamento, pagar, parcelar, parcela',
         'Aceitamos Pix, dinheiro e cartões de débito e crédito 😊 (resposta de exemplo)', 1),
        ('Tem estacionamento?', 'estacionamento, estacionar, carro, vaga, garagem',
         'Temos vagas na rua em frente ao salão 🚗 (resposta de exemplo)', 2),
        ('E se eu me atrasar?', 'atraso, atrasar, atrasada, atrasado, tolerancia',
         'Temos tolerância de 10 minutos. Depois disso, pode ser preciso remarcar para não atrasar as próximas clientes. (resposta de exemplo)', 3),
        ('Atende crianças?', 'crianca, infantil, filha, filho, menina, menino, kids',
         'Atendemos crianças, sim! É só agendar normalmente informando o serviço 💕 (resposta de exemplo)', 4)
       ) AS v(pergunta, palavras, resposta, ordem)
 WHERE NOT EXISTS (SELECT 1 FROM perguntas_frequentes p WHERE p.pergunta = v.pergunta);
