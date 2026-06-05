# Requisito: auditoria persistida no Fluxo B (`POST /ui/pedidos/gerar-automatico`)

Documento único de especificação para implementação (ou prompt de agente). Consolida blocos (I) e (II), limpeza, transação, entregáveis e critérios de aceite.

**Implementação (código):** Flyway `V4__fluxo_b_staging_auditoria.sql`; entidades em `pedidos.fluxo.audit`; `FluxoBAuditStagingService` (limpeza + persistência entrada); `FluxoPedidoAutomaticoService` grava staging de saída no loop das chaves; `LeitorNfeUcom.lerIdeCampos` / `NfeIdeCampos`; `LeitorResumoNf` + `ResumoNfLinhaDTO` estendidos para colunas opcionais do resumo.

---

## 1. Contexto

- **Endpoint:** `POST /ui/pedidos/gerar-automatico` — `UiPedidoController` → `FluxoPedidoAutomaticoService.gerarAutomatico`.
- **Stack:** Spring Boot, JPA/Hibernate, SQL Server em produção (migrações conforme padrão do projeto).
- **Fluxo atual (resumo):**
  - EFD: `ParserEfdService` → `EfdIndice`; chaves de NF-e de **saída** no ano/mês: `chavesSaidaNoMes`.
  - XML saída: pasta `ressarcimento.nfes-saida-dir`, `LeitorNfeUcom.localizarArquivoXml` + `listarItensComCfops` com CFOPs `6102` e `6108` (`CFOPS_FLUXO_B`).
  - Itens: `ItemNfeCfop(nItem, cfop, cProd, qCom, uCom)`.
  - Matriz: `ProdutoMatrizRepository` — no Fluxo B, `cProd` do XML de saída corresponde **somente** a `cod_interno_produto` (`findFirstByCodInternoProduto`); **não** há fallback por `cod_prod_fornecedor`.
  - Resumo: `LeitorResumoNf` → `ResumoNfLinhaDTO` (hoje: chave, seqItem, codgItem, cnpj, dataApresentacao, tributo, etc.).
  - FIFO + geração XML em memória: `GeradorXmlPedidos`; persistência atual: cabeçalho e caminhos do Fluxo B em `processamento_ressarcimento`; `log_execucao_fluxo`, `auditoria_produto_vendido`, `auditoria_entrada_consumida`, `arquivo_pedido` — **sem** gravar `nota_saida` / `item_nota_saida` / `nota_entrada` no Fluxo B.

---

## 2. Objetivo

Persistir **dois conjuntos de tabelas de staging/auditoria**:

1. **(I)** Resultado da busca de NF-e de **saída** (chaves do período + XML + CFOPs elegíveis) e itens, com vínculo a `produto_matriz`.
2. **(II)** NF-e de **entrada** identificadas pelo `resumonf.xlsx` (linhas filtradas), cabeçalho por chave + itens por linha do resumo.

**Limpeza:** esvaziar **todas** essas tabelas novas **antes** de iniciar o processamento desta API (início de `gerarAutomatico`), respeitando FKs.

---

## 3. Bloco (I) — NF-e de saída

**Momento:** após determinar a lista de chaves elegíveis (mesma lógica de período da EFD) e, para cada chave, após localizar o XML e filtrar itens por CFOP — persistir o resultado dessa busca.

**Decisão de escopo (recomendada):** gravar **cada chave candidata** do período com **status** ou flag (ex.: XML encontrado / não encontrado / sem itens CFOP), e preencher cabeçalho XML e itens quando aplicável.

### 3.1 Cabeçalho da nota (1 registro por NF-e de saída considerada)

- Chave de acesso (44 posições).
- **Datas/horários no XML da NF-e de saída:**
  - **Saída / movimentação:** persistir **`dhSaiEnt`**. Se a tag estiver ausente, definir regra (ex.: `null` + opcional log).
  - **Emissão:** persistir **`dhEmi`** do grupo `ide` (ou, por decisão explícita do projeto, só data via **`dEmi`**).
- Opcional: persistir também a data do documento na **EFD** (`EfdIndice.dataDocumentoSaida(chave)`) como campo auxiliar de conferência — **não** substitui `dhSaiEnt` / `dhEmi`/`dEmi`.
- **CFOP:** pode haver vários CFOPs na mesma nota. Escolher modelo e documentar: **(a)** CFOP só nas linhas de item; **(b)** na nota, lista ordenada / texto agregado dos CFOPs dos itens elegíveis; **(c)** ambos.

### 3.2 Itens da nota de saída

Alinhado ao retorno de `LeitorNfeUcom.listarItensComCfops` (ou equivalente) para CFOPs elegíveis:

- Código do item no XML (`cProd`).
- Sequência no XML (`nItem`).
- Quantidade vendida (`qCom`, unidade comercial do XML).

### 3.3 Relação com `produto_matriz`

- Regra de mapeamento: **`cProd` (XML de saída) = `cod_interno_produto`** na tabela `produto_matriz`. Não comparar `cProd` com `cod_prod_fornecedor`.
- Por item: FK para o registro resolvido por `resolverProdutoPorCProdSaida` (ex.: `produto_matriz.id`). Se existirem **várias** linhas com o mesmo `cod_interno_produto`, usar a mesma desambiguação do código (ex.: `findFirstByCodInternoProduto`); documentar no código se a ordem for relevante.
- Se não houver linha com esse código interno, FK nula e/ou flag “sem matriz”.
- Opcional: redundância de `cod_interno_produto` copiado da linha resolvida para relatórios.

### 3.4 Limpeza (I)

Antes de processar, apagar registros das tabelas do bloco **(I)** na ordem correta (filhos → pais: itens / vínculos → notas).

---

## 4. Bloco (II) — Entradas a partir do `resumonf.xlsx`

**Momento:** após ler o `.xlsx` e aplicar filtro de ano/mês (e demais filtros já usados no Fluxo B, ex. tributo).

### 4.1 Cabeçalho da nota de entrada (1 registro por chave 44 distinta nas linhas filtradas)

- **Chave de acesso:** coluna **CHAVE** do resumo.
- **Número da nota:** coluna **`NR. NOTA`** do `resumonf.xlsx` (cabeçalho normalizado como em `LeitorResumoNf.normalizarTituloColuna`).
- **Data de apresentação:** coluna **DATA APRES.**
- **Data de emissão:** XML da NF-e de **entrada** (pasta `ressarcimento.nfes-dir`, localização por chave como no restante do projeto): prioridade **`dhEmi`**, senão **`dEmi`** — documentar. Se XML não encontrado: `null` + log (ex.: `log_execucao_fluxo`).

### 4.2 Itens (1 registro por linha relevante do resumo)

Preencher **somente** a partir do `resumonf.xlsx`, via **`ResumoNfLinhaDTO`** (ou DTO equivalente), **estendendo** leitor e DTO onde ainda não existam:

| Campo lógico | Fonte |
|--------------|--------|
| Código do item | Planilha (ex.: **CODG. ITEM**) |
| Sequência do item | Planilha (**SEQ. ITEM**) |
| Tributo | Planilha (**TRIBUTO**) |
| Quantidade unitária comprada | Planilha — mapear coluna real |
| Valor unitário | Planilha — mapear coluna real |
| CFOP | Planilha — mapear coluna real |
| Valor do imposto | Planilha — mapear coluna real |
| CNPJ fornecedor | Planilha (**CNPJ FORNECEDOR**) |

**Implementação:** atualizar `LeitorResumoNf` para ler **`NR. NOTA`** e todas as colunas necessárias; ajustar validação de cabeçalhos obrigatórios. Para emissão da **nota**, implementar leitura de XML de entrada (reutilizar ou extrair de `LeitorNfeUcom` método que, dada chave + pasta de NF-e entrada, devolva `dhEmi`/`dEmi`), com **cache por chave** na execução.

### 4.3 Limpeza (II)

Junto com (I), esvaziar tabelas do bloco **(II)** (itens → notas) no início de `gerarAutomatico`.

---

## 5. Limpeza global e ordem

- **Quando:** imediatamente no início do processamento de `gerarAutomatico` (após validações de diretórios, se desejado, ou no primeiro passo da transação — documentar).
- **Escopo:** substituição do conteúdo das tabelas de staging a **cada** execução (global por instalação), salvo decisão futura explícita de escopo por `declarante_id` ou período.
- **Ordem sugerida (ajustar aos nomes reais das FKs):**
  1. Filhos de (I): itens / vínculos matriz.
  2. Pais de (I): notas de saída staging.
  3. Filhos de (II): itens entrada staging.
  4. Pais de (II): notas entrada staging.

---

## 6. Transação

- Definir se limpeza + repovoamento + restante do fluxo (execução, XML, `arquivo_pedido`) ficam na **mesma** `@Transactional` ou se staging em transação curta e fluxo longo em outra — avaliar tempo de I/O em disco (EFD/XML).
- Garantir que falha no meio não deixe staging inconsistente com a intenção do requisito (“última execução bem-sucedida” vs. “estado parcial”).

---

## 7. Entregáveis técnicos

- Novas entidades JPA + `@Table` + repositórios.
- Migração SQL (Flyway/Liquibase conforme o projeto).
- Alterações em `FluxoPedidoAutomaticoService.gerarAutomatico`.
- Extensões em `LeitorNfeUcom` (ou serviço auxiliar) para `dhSaiEnt`, `dhEmi`/`dEmi` **saída** e leitura de **emissão** no XML de **entrada**.
- Extensões em `LeitorResumoNf` e `ResumoNfLinhaDTO` para **NR. NOTA** e colunas dos itens (II).
- Testes: pelo menos um teste que verifique limpeza + gravação mínima (mocks de arquivo/stream onde fizer sentido).

---

## 8. Critérios de aceite

- Ao executar `gerar-automatico`, as tabelas de staging (I) e (II) são **esvaziadas no início** e refletem **apenas** os dados da rodada (ou política documentada em §6).
- **(I):** chaves/itens alinhados ao filtro EFD + CFOP + XML de saída; datas de saída/emissão conforme **`dhSaiEnt`** e **`dhEmi`/`dEmi`** do XML de saída; vínculo `produto_matriz` quando existir linha com **`cod_interno_produto` = `cProd`** (sem uso de `cod_prod_fornecedor` para esse mapeamento).
- **(II):** cabeçalhos com **NR. NOTA** e **DATA APRES.** do resumo; emissão do XML de entrada; itens com campos vindos **exclusivamente** da planilha (DTO estendido).
- Documentação no código das colunas exatas do Excel mapeadas para cada campo novo.

---

## 9. Referências no código (ponto de partida)

- `FluxoPedidoAutomaticoService` — orquestração Fluxo B.
- `LeitorNfeUcom` — XML saída, itens por CFOP.
- `ParserEfdService` / `EfdIndice` — chaves e `dataDocumentoSaida`.
- `LeitorResumoNf` / `ResumoNfLinhaDTO` — resumo NF.
- `RessarcimentoProperties` — pastas EFD, NF-e saída, NF-e entrada, resumo.
