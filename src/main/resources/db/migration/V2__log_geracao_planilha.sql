-- Logs de inconsistências na geração automática da Planilha Produtos (Prompt 22-03-2026)
CREATE TABLE log_geracao_planilha (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    tipo NVARCHAR(80) NOT NULL,
    chave_nfe CHAR(44) NULL,
    num_item INT NULL,
    data_processamento DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    arquivo_origem NVARCHAR(500) NULL,
    mensagem NVARCHAR(MAX) NOT NULL
);

CREATE INDEX ix_log_geracao_planilha_data ON log_geracao_planilha(data_processamento DESC);
CREATE INDEX ix_log_geracao_planilha_tipo ON log_geracao_planilha(tipo);
