-- Cabeçalho de um processamento orquestrado (ex.: tela Processar Ressarcimento)
CREATE TABLE processamento_ressarcimento (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    declarante_id BIGINT NOT NULL,
    ano_referencia CHAR(4) NOT NULL,
    mes_referencia CHAR(2) NOT NULL,
    data_hora_inicio DATETIME2 NOT NULL,
    data_hora_fim DATETIME2 NULL,
    status_execucao NVARCHAR(40) NOT NULL,
    mensagem_erro NVARCHAR(2000) NULL,
    CONSTRAINT fk_proc_ress_declarante FOREIGN KEY (declarante_id) REFERENCES declarante(id)
);

CREATE INDEX ix_proc_ress_decl_periodo ON processamento_ressarcimento(declarante_id, ano_referencia, mes_referencia);

ALTER TABLE execucao_fluxo_pedido ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE execucao_fluxo_pedido ADD CONSTRAINT fk_exec_fluxo_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);

CREATE INDEX ix_exec_fluxo_proc_ress ON execucao_fluxo_pedido(processamento_ressarcimento_id);

ALTER TABLE arquivo_pedido ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE arquivo_pedido ADD CONSTRAINT fk_arq_ped_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);

CREATE INDEX ix_arq_ped_proc_ress ON arquivo_pedido(processamento_ressarcimento_id);

ALTER TABLE arquivo_produtos ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE arquivo_produtos ADD CONSTRAINT fk_arq_prod_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);

CREATE INDEX ix_arq_prod_proc_ress ON arquivo_produtos(processamento_ressarcimento_id);

ALTER TABLE log_geracao_planilha ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE log_geracao_planilha ADD CONSTRAINT fk_log_plan_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);

CREATE INDEX ix_log_plan_proc_ress ON log_geracao_planilha(processamento_ressarcimento_id);

ALTER TABLE nota_saida ADD processamento_ressarcimento_id BIGINT NULL;
ALTER TABLE nota_saida ADD CONSTRAINT fk_nota_saida_proc_ress
    FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);

CREATE INDEX ix_nota_saida_proc_ress ON nota_saida(processamento_ressarcimento_id);
