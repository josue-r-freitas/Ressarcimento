-- Staging de auditoria do Fluxo B (limpo a cada execução de gerar-automatico)
CREATE TABLE fluxo_b_audit_nfe_saida (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    chave_nfe CHAR(44) NOT NULL,
    status_processamento NVARCHAR(40) NOT NULL,
    dh_sai_ent NVARCHAR(35) NULL,
    dh_emi NVARCHAR(35) NULL,
    d_emi NVARCHAR(12) NULL,
    data_doc_efd DATE NULL,
    cfops_itens_elegiveis NVARCHAR(200) NULL
);

CREATE INDEX ix_fluxo_b_audit_nfe_saida_chave ON fluxo_b_audit_nfe_saida(chave_nfe);

CREATE TABLE fluxo_b_audit_item_nfe_saida (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    audit_nfe_saida_id BIGINT NOT NULL,
    num_item_nfe INT NOT NULL,
    c_prod NVARCHAR(60) NOT NULL,
    cfop CHAR(4) NOT NULL,
    q_com DECIMAL(15,6) NOT NULL,
    produto_matriz_id BIGINT NULL,
    cod_interno_resolvido NVARCHAR(60) NULL,
    CONSTRAINT fk_fluxo_b_item_saida_nota FOREIGN KEY (audit_nfe_saida_id) REFERENCES fluxo_b_audit_nfe_saida(id) ON DELETE CASCADE,
    CONSTRAINT fk_fluxo_b_item_saida_matriz FOREIGN KEY (produto_matriz_id) REFERENCES produto_matriz(id)
);

CREATE INDEX ix_fluxo_b_item_saida_nota ON fluxo_b_audit_item_nfe_saida(audit_nfe_saida_id);

CREATE TABLE fluxo_b_audit_nfe_entrada (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    chave_nfe CHAR(44) NOT NULL,
    nr_nota NVARCHAR(20) NULL,
    data_apresentacao DATE NULL,
    dh_emi NVARCHAR(35) NULL,
    d_emi NVARCHAR(12) NULL
);

CREATE UNIQUE INDEX ux_fluxo_b_audit_nfe_entrada_chave ON fluxo_b_audit_nfe_entrada(chave_nfe);

CREATE TABLE fluxo_b_audit_item_nfe_entrada (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    audit_nfe_entrada_id BIGINT NOT NULL,
    seq_item INT NOT NULL,
    codg_item NVARCHAR(60) NULL,
    tributo NVARCHAR(20) NULL,
    qtd_unit_compra DECIMAL(15,6) NULL,
    valor_unitario DECIMAL(15,6) NULL,
    cfop CHAR(4) NULL,
    valor_imposto DECIMAL(15,6) NULL,
    cnpj_fornecedor CHAR(14) NULL,
    numero_linha_planilha INT NULL,
    CONSTRAINT fk_fluxo_b_item_entrada_nota FOREIGN KEY (audit_nfe_entrada_id) REFERENCES fluxo_b_audit_nfe_entrada(id) ON DELETE CASCADE
);

CREATE INDEX ix_fluxo_b_item_entrada_nota ON fluxo_b_audit_item_nfe_entrada(audit_nfe_entrada_id);
