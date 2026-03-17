-- Tabela declarante (dados do contribuinte para dadosDeclarante nos XMLs)
CREATE TABLE declarante (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cnpj_raiz CHAR(8) NOT NULL,
    ie_contribuinte NVARCHAR(15) NOT NULL,
    razao_social NVARCHAR(60) NOT NULL,
    nome_responsavel NVARCHAR(60) NOT NULL,
    fone_responsavel NVARCHAR(15) NOT NULL,
    email_responsavel NVARCHAR(60) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE UNIQUE INDEX ix_declarante_cnpj_raiz ON declarante(cnpj_raiz);

-- Tabela produto_matriz (MATRI-NAC)
CREATE TABLE produto_matriz (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cod_interno_produto NVARCHAR(60) NOT NULL,
    descricao_produto NVARCHAR(100) NOT NULL,
    unidade_interna_produto NVARCHAR(8) NOT NULL,
    fator_conversao DECIMAL(15,6) NOT NULL,
    cnpj_fornecedor CHAR(14) NOT NULL,
    cod_prod_fornecedor NVARCHAR(60) NOT NULL,
    unidade_produto_fornecedor NVARCHAR(8) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE INDEX ix_produto_matriz_cod_interno ON produto_matriz(cod_interno_produto);

-- Tabela arquivo_produtos (histórico de arquivos de produtos gerados)
CREATE TABLE arquivo_produtos (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    declarante_id BIGINT NOT NULL,
    data_geracao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    status NVARCHAR(30) NOT NULL DEFAULT N'GERADO',
    mensagem_log NVARCHAR(MAX) NULL,
    xml_content NVARCHAR(MAX) NULL,
    CONSTRAINT fk_arquivo_produtos_declarante FOREIGN KEY (declarante_id) REFERENCES declarante(id)
);

CREATE INDEX ix_arquivo_produtos_declarante ON arquivo_produtos(declarante_id);
CREATE INDEX ix_arquivo_produtos_data ON arquivo_produtos(data_geracao);

-- Tabela nota_saida (operações de saída por chave NF-e)
CREATE TABLE nota_saida (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    declarante_id BIGINT NOT NULL,
    chave_nfe CHAR(44) NOT NULL,
    ano_periodo_referencia CHAR(4) NOT NULL,
    mes_periodo_referencia CHAR(2) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_nota_saida_declarante FOREIGN KEY (declarante_id) REFERENCES declarante(id)
);

CREATE UNIQUE INDEX ix_nota_saida_chave ON nota_saida(chave_nfe);
CREATE INDEX ix_nota_saida_periodo ON nota_saida(ano_periodo_referencia, mes_periodo_referencia);

-- Tabela nota_entrada (chaves de NF-e/CT-e/MDF-e de entrada)
CREATE TABLE nota_entrada (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    chave_nfe_entrada CHAR(44) NOT NULL,
    chave_cte_entrada CHAR(44) NULL,
    chave_mdfe_entrada CHAR(44) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);

CREATE UNIQUE INDEX ix_nota_entrada_chave ON nota_entrada(chave_nfe_entrada);

-- Tabela item_nota_saida (itens da NF-e de saída)
CREATE TABLE item_nota_saida (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    nota_saida_id BIGINT NOT NULL,
    produto_matriz_id BIGINT NULL,
    cod_interno_produto NVARCHAR(60) NOT NULL,
    num_item_nfe INT NOT NULL,
    nota_entrada_id BIGINT NULL,
    CONSTRAINT fk_item_nota_saida_nota FOREIGN KEY (nota_saida_id) REFERENCES nota_saida(id),
    CONSTRAINT fk_item_nota_saida_produto FOREIGN KEY (produto_matriz_id) REFERENCES produto_matriz(id),
    CONSTRAINT fk_item_nota_saida_nota_entrada FOREIGN KEY (nota_entrada_id) REFERENCES nota_entrada(id)
);

CREATE INDEX ix_item_nota_saida_nota ON item_nota_saida(nota_saida_id);

-- Tabela arquivo_pedido (histórico de arquivos de pedidos gerados)
CREATE TABLE arquivo_pedido (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    declarante_id BIGINT NOT NULL,
    ano_referencia CHAR(4) NOT NULL,
    mes_referencia CHAR(2) NOT NULL,
    data_geracao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    status NVARCHAR(30) NOT NULL DEFAULT N'GERADO',
    mensagem_log NVARCHAR(MAX) NULL,
    xml_content NVARCHAR(MAX) NULL,
    CONSTRAINT fk_arquivo_pedido_declarante FOREIGN KEY (declarante_id) REFERENCES declarante(id)
);

CREATE INDEX ix_arquivo_pedido_declarante ON arquivo_pedido(declarante_id);
CREATE INDEX ix_arquivo_pedido_periodo ON arquivo_pedido(ano_referencia, mes_referencia);
