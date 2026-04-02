-- Rastreabilidade do Fluxo B (geração automática de XML de pedidos)
CREATE TABLE execucao_fluxo_pedido (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    declarante_id BIGINT NOT NULL,
    ano_referencia CHAR(4) NOT NULL,
    mes_referencia CHAR(2) NOT NULL,
    data_hora_inicio DATETIME2 NOT NULL,
    data_hora_fim DATETIME2 NULL,
    status_execucao NVARCHAR(40) NOT NULL,
    arquivo_efd_utilizado NVARCHAR(500) NULL,
    pasta_nfes_saida NVARCHAR(500) NULL,
    pasta_nfes_entrada NVARCHAR(500) NULL,
    arquivo_resumonf NVARCHAR(500) NULL,
    CONSTRAINT fk_exec_fluxo_pedido_declarante FOREIGN KEY (declarante_id) REFERENCES declarante(id)
);

CREATE INDEX ix_exec_fluxo_pedido_decl_periodo ON execucao_fluxo_pedido(declarante_id, ano_referencia, mes_referencia);

CREATE TABLE auditoria_produto_vendido (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    execucao_id BIGINT NOT NULL,
    cod_interno_produto NVARCHAR(60) NOT NULL,
    chave_nfe_saida CHAR(44) NOT NULL,
    num_item_nfe INT NOT NULL,
    cfop_item CHAR(4) NULL,
    quantidade_vendida_unidade_interna DECIMAL(15,6) NOT NULL,
    unidade_interna NVARCHAR(8) NOT NULL,
    quantidade_total_compras_convertida DECIMAL(15,6) NULL,
    suficiente BIT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aud_prod_vend_exec FOREIGN KEY (execucao_id) REFERENCES execucao_fluxo_pedido(id) ON DELETE CASCADE
);

CREATE INDEX ix_aud_prod_vend_exec ON auditoria_produto_vendido(execucao_id);

CREATE TABLE auditoria_entrada_consumida (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    auditoria_produto_id BIGINT NOT NULL,
    chave_nfe_entrada CHAR(44) NOT NULL,
    seq_item INT NOT NULL,
    codg_item NVARCHAR(60) NULL,
    cnpj_fornecedor CHAR(14) NULL,
    tributo NVARCHAR(10) NULL,
    data_apresentacao DATE NULL,
    quantidade_original_unidade_fornecedor DECIMAL(15,6) NULL,
    unidade_fornecedor NVARCHAR(8) NULL,
    fator_conversao_aplicado DECIMAL(15,6) NULL,
    quantidade_convertida_unidade_interna DECIMAL(15,6) NULL,
    conversao_aplicada BIT NOT NULL DEFAULT 0,
    quantidade_consumida DECIMAL(15,6) NULL,
    CONSTRAINT fk_aud_ent_cons_prod FOREIGN KEY (auditoria_produto_id) REFERENCES auditoria_produto_vendido(id) ON DELETE CASCADE
);

CREATE INDEX ix_aud_ent_cons_prod ON auditoria_entrada_consumida(auditoria_produto_id);

CREATE TABLE log_execucao_fluxo (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    execucao_id BIGINT NOT NULL,
    nivel NVARCHAR(10) NOT NULL,
    etapa NVARCHAR(100) NOT NULL,
    mensagem NVARCHAR(1000) NOT NULL,
    detalhes NVARCHAR(MAX) NULL,
    ts DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_log_exec_fluxo FOREIGN KEY (execucao_id) REFERENCES execucao_fluxo_pedido(id) ON DELETE CASCADE
);

CREATE INDEX ix_log_exec_fluxo_exec ON log_execucao_fluxo(execucao_id);

ALTER TABLE arquivo_pedido ADD execucao_fluxo_id BIGINT NULL;
ALTER TABLE arquivo_pedido ADD CONSTRAINT fk_arquivo_pedido_exec_fluxo
    FOREIGN KEY (execucao_fluxo_id) REFERENCES execucao_fluxo_pedido(id);
