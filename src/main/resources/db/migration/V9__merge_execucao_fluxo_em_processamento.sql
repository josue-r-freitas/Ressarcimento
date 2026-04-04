-- Absorve execucao_fluxo_pedido em processamento_ressarcimento (Fluxo B).
-- Batches separados com GO: SQL Server não enxerga colunas novas no mesmo batch do ALTER ADD.

ALTER TABLE processamento_ressarcimento ADD arquivo_efd_utilizado NVARCHAR(500) NULL;
ALTER TABLE processamento_ressarcimento ADD pasta_nfes_saida NVARCHAR(500) NULL;
ALTER TABLE processamento_ressarcimento ADD pasta_nfes_entrada NVARCHAR(500) NULL;
ALTER TABLE processamento_ressarcimento ADD arquivo_resumonf NVARCHAR(500) NULL;
GO

UPDATE p
SET
    arquivo_efd_utilizado = e.arquivo_efd_utilizado,
    pasta_nfes_saida = e.pasta_nfes_saida,
    pasta_nfes_entrada = e.pasta_nfes_entrada,
    arquivo_resumonf = e.arquivo_resumonf
FROM processamento_ressarcimento p
INNER JOIN execucao_fluxo_pedido e ON e.id = (
    SELECT MAX(ef.id)
    FROM execucao_fluxo_pedido ef
    WHERE ef.processamento_ressarcimento_id = p.id
);
GO

ALTER TABLE auditoria_produto_vendido DROP CONSTRAINT fk_aud_prod_vend_exec;
DROP INDEX IF EXISTS ix_aud_prod_vend_exec ON auditoria_produto_vendido;
ALTER TABLE auditoria_produto_vendido DROP COLUMN execucao_id;
GO

ALTER TABLE log_execucao_fluxo ADD processamento_ressarcimento_id BIGINT NULL;
GO

UPDATE lef
SET processamento_ressarcimento_id = ef.processamento_ressarcimento_id
FROM log_execucao_fluxo lef
INNER JOIN execucao_fluxo_pedido ef ON ef.id = lef.execucao_id;
GO

ALTER TABLE log_execucao_fluxo ALTER COLUMN processamento_ressarcimento_id BIGINT NOT NULL;
GO

ALTER TABLE log_execucao_fluxo DROP CONSTRAINT fk_log_exec_fluxo;
DROP INDEX IF EXISTS ix_log_exec_fluxo_exec ON log_execucao_fluxo;
ALTER TABLE log_execucao_fluxo DROP COLUMN execucao_id;
GO

ALTER TABLE log_execucao_fluxo ADD CONSTRAINT fk_log_exec_fluxo_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id) ON DELETE CASCADE;
CREATE INDEX ix_log_exec_fluxo_proc_ress ON log_execucao_fluxo(processamento_ressarcimento_id);
GO

ALTER TABLE arquivo_pedido DROP CONSTRAINT fk_arquivo_pedido_exec_fluxo;
ALTER TABLE arquivo_pedido DROP COLUMN execucao_fluxo_id;
GO

DROP INDEX IF EXISTS ix_exec_fluxo_proc_ress ON execucao_fluxo_pedido;
ALTER TABLE execucao_fluxo_pedido DROP CONSTRAINT fk_exec_fluxo_proc_ress;
DROP INDEX IF EXISTS ix_exec_fluxo_pedido_decl_periodo ON execucao_fluxo_pedido;
ALTER TABLE execucao_fluxo_pedido DROP CONSTRAINT fk_exec_fluxo_pedido_declarante;
DROP TABLE execucao_fluxo_pedido;
