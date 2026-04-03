-- Torna processamento_ressarcimento_id obrigatório nas tabelas rastreadas.
-- Remove registos legados sem processamento (sem rastreio válido).

DELETE FROM arquivo_pedido
WHERE execucao_fluxo_id IN (
    SELECT id FROM execucao_fluxo_pedido WHERE processamento_ressarcimento_id IS NULL
);

DELETE FROM log_execucao_fluxo
WHERE execucao_id IN (SELECT id FROM execucao_fluxo_pedido WHERE processamento_ressarcimento_id IS NULL);

DELETE FROM auditoria_entrada_consumida WHERE processamento_ressarcimento_id IS NULL;

DELETE FROM auditoria_produto_vendido
WHERE execucao_id IN (SELECT id FROM execucao_fluxo_pedido WHERE processamento_ressarcimento_id IS NULL)
   OR processamento_ressarcimento_id IS NULL;

DELETE FROM execucao_fluxo_pedido WHERE processamento_ressarcimento_id IS NULL;

DELETE FROM fluxo_b_audit_item_nfe_saida
WHERE audit_nfe_saida_id IN (
    SELECT id FROM fluxo_b_audit_nfe_saida WHERE processamento_ressarcimento_id IS NULL
);
DELETE FROM fluxo_b_audit_item_nfe_entrada
WHERE audit_nfe_entrada_id IN (
    SELECT id FROM fluxo_b_audit_nfe_entrada WHERE processamento_ressarcimento_id IS NULL
);
DELETE FROM fluxo_b_audit_nfe_saida WHERE processamento_ressarcimento_id IS NULL;
DELETE FROM fluxo_b_audit_nfe_entrada WHERE processamento_ressarcimento_id IS NULL;

DELETE FROM arquivo_pedido WHERE processamento_ressarcimento_id IS NULL;
DELETE FROM arquivo_produtos WHERE processamento_ressarcimento_id IS NULL;
DELETE FROM log_geracao_planilha WHERE processamento_ressarcimento_id IS NULL;

DELETE FROM item_nota_saida WHERE nota_saida_id IN (
    SELECT id FROM nota_saida WHERE processamento_ressarcimento_id IS NULL
);
DELETE FROM nota_saida WHERE processamento_ressarcimento_id IS NULL;

DELETE FROM fluxo_b_audit_item_nfe_saida
WHERE produto_matriz_id IN (SELECT id FROM produto_matriz WHERE processamento_ressarcimento_id IS NULL);

UPDATE item_nota_saida SET produto_matriz_id = NULL
WHERE produto_matriz_id IN (SELECT id FROM produto_matriz WHERE processamento_ressarcimento_id IS NULL);

DELETE FROM item_nota_saida WHERE nota_entrada_id IN (
    SELECT id FROM nota_entrada WHERE processamento_ressarcimento_id IS NULL
);
DELETE FROM nota_entrada WHERE processamento_ressarcimento_id IS NULL;

DELETE FROM produto_matriz WHERE processamento_ressarcimento_id IS NULL;

-- SQL Server nao permite ALTER COLUMN ... NOT NULL com indice na coluna (erro 5074).
DROP INDEX IF EXISTS ix_exec_fluxo_proc_ress ON execucao_fluxo_pedido;
DROP INDEX IF EXISTS ix_arq_ped_proc_ress ON arquivo_pedido;
DROP INDEX IF EXISTS ix_arq_prod_proc_ress ON arquivo_produtos;
DROP INDEX IF EXISTS ix_log_plan_proc_ress ON log_geracao_planilha;
DROP INDEX IF EXISTS ix_nota_saida_proc_ress ON nota_saida;
DROP INDEX IF EXISTS ix_produto_matriz_proc_ress ON produto_matriz;
DROP INDEX IF EXISTS ix_nota_entrada_proc_ress ON nota_entrada;
DROP INDEX IF EXISTS ix_fluxo_b_audit_nfe_ent_proc ON fluxo_b_audit_nfe_entrada;
DROP INDEX IF EXISTS ix_fluxo_b_audit_nfe_sai_proc ON fluxo_b_audit_nfe_saida;
DROP INDEX IF EXISTS ix_aud_prod_vend_proc_ress ON auditoria_produto_vendido;
DROP INDEX IF EXISTS ix_aud_ent_cons_proc_ress ON auditoria_entrada_consumida;

ALTER TABLE execucao_fluxo_pedido ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE arquivo_pedido ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE arquivo_produtos ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE log_geracao_planilha ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE nota_saida ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;

ALTER TABLE produto_matriz ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE nota_entrada ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE fluxo_b_audit_nfe_entrada ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE fluxo_b_audit_nfe_saida ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE auditoria_produto_vendido ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
ALTER TABLE auditoria_entrada_consumida ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;

CREATE INDEX ix_exec_fluxo_proc_ress ON execucao_fluxo_pedido(processamento_ressarcimento_id);
CREATE INDEX ix_arq_ped_proc_ress ON arquivo_pedido(processamento_ressarcimento_id);
CREATE INDEX ix_arq_prod_proc_ress ON arquivo_produtos(processamento_ressarcimento_id);
CREATE INDEX ix_log_plan_proc_ress ON log_geracao_planilha(processamento_ressarcimento_id);
CREATE INDEX ix_nota_saida_proc_ress ON nota_saida(processamento_ressarcimento_id);
CREATE INDEX ix_produto_matriz_proc_ress ON produto_matriz(processamento_ressarcimento_id);
CREATE INDEX ix_nota_entrada_proc_ress ON nota_entrada(processamento_ressarcimento_id);
CREATE INDEX ix_fluxo_b_audit_nfe_ent_proc ON fluxo_b_audit_nfe_entrada(processamento_ressarcimento_id);
CREATE INDEX ix_fluxo_b_audit_nfe_sai_proc ON fluxo_b_audit_nfe_saida(processamento_ressarcimento_id);
CREATE INDEX ix_aud_prod_vend_proc_ress ON auditoria_produto_vendido(processamento_ressarcimento_id);
CREATE INDEX ix_aud_ent_cons_proc_ress ON auditoria_entrada_consumida(processamento_ressarcimento_id);
