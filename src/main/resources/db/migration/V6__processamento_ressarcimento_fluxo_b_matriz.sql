-- Rastreio por processamento (alinhado a arquivo_pedido / arquivo_produtos / nota_saida)

ALTER TABLE produto_matriz ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE produto_matriz ADD CONSTRAINT fk_produto_matriz_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);
CREATE INDEX ix_produto_matriz_proc_ress ON produto_matriz(processamento_ressarcimento_id);

ALTER TABLE nota_entrada ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE nota_entrada ADD CONSTRAINT fk_nota_entrada_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);
CREATE INDEX ix_nota_entrada_proc_ress ON nota_entrada(processamento_ressarcimento_id);

ALTER TABLE fluxo_b_audit_nfe_entrada ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE fluxo_b_audit_nfe_entrada ADD CONSTRAINT fk_fluxo_b_audit_nfe_ent_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);
CREATE INDEX ix_fluxo_b_audit_nfe_ent_proc ON fluxo_b_audit_nfe_entrada(processamento_ressarcimento_id);

ALTER TABLE fluxo_b_audit_nfe_saida ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE fluxo_b_audit_nfe_saida ADD CONSTRAINT fk_fluxo_b_audit_nfe_sai_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);
CREATE INDEX ix_fluxo_b_audit_nfe_sai_proc ON fluxo_b_audit_nfe_saida(processamento_ressarcimento_id);

ALTER TABLE auditoria_produto_vendido ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE auditoria_produto_vendido ADD CONSTRAINT fk_aud_prod_vend_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);
CREATE INDEX ix_aud_prod_vend_proc_ress ON auditoria_produto_vendido(processamento_ressarcimento_id);

ALTER TABLE auditoria_entrada_consumida ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE auditoria_entrada_consumida ADD CONSTRAINT fk_aud_ent_cons_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);
CREATE INDEX ix_aud_ent_cons_proc_ress ON auditoria_entrada_consumida(processamento_ressarcimento_id);
