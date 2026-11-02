-- =============================================================================
-- V8 - Exclusão definitiva: remove as clientes que tinham sido só anonimizadas
-- ("Cliente removida") e os agendamentos delas.
-- =============================================================================
DELETE FROM agendamentos
 WHERE cliente_id IN (SELECT id FROM clientes WHERE anonimizado_em IS NOT NULL);

DELETE FROM clientes
 WHERE anonimizado_em IS NOT NULL;
