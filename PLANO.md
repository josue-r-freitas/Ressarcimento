# Plano atualizado – Sistema de Ressarcimento ICMS-ST (SEFAZ/AM)

Documento de referência do que **já está implementado** no projeto `ressarcimento-st` (atualização em março/2026).

---

## 1. Objetivo do sistema

Aplicação **Spring Boot** para apoiar o fluxo de **ressarcimento de ICMS-ST** (manual SEFAZ/AM / envio via DT-e):

- Cadastro do **declarante** (dados para `dadosDeclarante` nos XMLs).
- Importação de **planilhas** (Excel `.xlsx` ou CSV) de **produtos** (MATRI-NAC) e de **operações de saída** (pedidos).
- Persistência em **SQL Server** com **Flyway**.
- Geração de XML via **JAXB** (`enviProdutoRessarcimento` e `enviOperacaoRessarcimento`).
- **Histórico** de XMLs gerados com **download** posterior.

---

## 2. Stack e infraestrutura

| Item | Detalhe |
|------|---------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Web, JPA, Validation | starters oficiais |
| Banco | Microsoft SQL Server (`mssql-jdbc`) |
| Migrações | Flyway (`flyway-sqlserver`) |
| Planilhas | Apache POI 5.2.5 (Excel) + leitura CSV |
| XML | Jakarta XML Bind + JAXB runtime |
| API docs | Springdoc OpenAPI 2.3.0 (`/swagger-ui.html`, `/v3/api-docs`) |
| Testes | JUnit 5, Spring Boot Test, **H2** (testes locais), **Testcontainers** (SQL Server em integração) |
| Build | Maven (`pom.xml`) |

**Configuração:** `application.yml` (URL do SQL Server, JPA `ddl-auto: validate`, Flyway habilitado). Recomenda-se **não versionar senhas**; usar variáveis de ambiente em produção.

---

## 3. Modelo de dados (implementado)

Scripts: `src/main/resources/db/migration/V1__criar_tabelas_basicas.sql`, `V2__log_geracao_planilha.sql`

- **`declarante`** – único por `cnpj_raiz` (índice único).
- **`produto_matriz`** – cadastro MATRI-NAC.
- **`arquivo_produtos`** – histórico de XML de produtos (`xml_content`, status, vínculo ao declarante).
- **`nota_saida`** – NF-e de saída (chave 44 posições, período ano/mês, declarante).
- **`nota_entrada`** – chaves NF-e/CT-e/MDF-e de entrada.
- **`item_nota_saida`** – itens da saída (código interno, nº item, vínculo opcional a `produto_matriz` e `nota_entrada`).
- **`arquivo_pedido`** – histórico de XML de pedidos por período (ano/mês).

Entidades JPA correspondentes nos pacotes `declarante.domain`, `produtos.domain`, `pedidos.domain`.

---

## 4. Módulos e responsabilidades

### 4.1 Declarante (`/api/declarante`)

- **POST** – criar ou atualizar por `cnpj_raiz` (demais campos atualizados se já existir).
- **GET** – retorna o primeiro declarante cadastrado; se não houver, **404** (`DeclaranteNaoEncontradoException`).

Serviço expõe `getEntidadeOuLanca()` usado por produtos e pedidos.

### 4.2 Produtos (`/api/produtos`)

- **POST `/importar`** – multipart `arquivo` (CSV ou Excel); validação Bean Validation em `ProdutoPlanilhaDTO`; em caso de erro, retorna `ResultadoImportacaoDTO` com lista de `ErroPlanilhaDTO` **sem persistir** nada.
- **GET `/`** – listagem paginada; filtros opcionais `codigo`, `descricao`.
- **POST `/gerar-xml`** – monta XML com todos os produtos ordenados por código interno + declarante; grava em `arquivo_produtos`; retorna arquivo para download (`enviProdutoRessarcimento.xml`).
- **GET `/historico`** – histórico paginado do declarante.
- **GET `/historico/{id}/download`** – download do XML salvo no histórico.
- **POST `/gerar-planilha-automatica`** – gera Excel da Planilha Produtos a partir das pastas configuradas em `ressarcimento.resumo-notas-dir`, `ressarcimento.efds-dir` e `ressarcimento.nfes-dir` (ou variáveis `RESSARCIMENTO_*`). Body JSON opcional: `anoReferencia`, `mesReferencia`, `nomeArquivoResumo`.
- **GET `/logs-geracao-planilha`** – paginação Spring (`page`, `size`) com logs de inconsistências (`log_geracao_planilha`, Flyway `V2`).

Componentes: `LeitorPlanilhaProdutos`, `ValidacaoPlanilhaUtil`, `GeradorXmlProdutos`, `EnviProdutoRessarcimento` (JAXB), além do módulo `produtos.automatizado` (`ParserEfdService`, `LeitorResumoNf`, `LeitorNfeUcom`, `ProdutoPlanilhaAutomaticaService`, `EscritorPlanilhaProdutosExcel`).

### 4.3 Pedidos / operações (`/api/pedidos`)

- **POST `/importar`** – lê `OperacaoPlanilhaDTO`; validação (`ValidacaoPlanilhaOperacoes` + constraints no DTO); **todas as linhas devem ter o mesmo ano e mês de referência**; agrupa por chave NF-e saída; cria/atualiza `NotaSaida`, `NotaEntrada` (se chave entrada informada), `ItemNotaSaida`; associa `ProdutoMatriz` quando existe código interno cadastrado (pode ficar `null`).
- **GET `/`** – listagem paginada de notas de saída do declarante; filtros opcionais `ano`, `mes`.
- **POST `/gerar-xml`** – query obrigatória `ano`, `mes`; gera XML do período; persiste em `arquivo_pedido`.
- **GET `/historico`** e **GET `/historico/{id}/download`** – análogo aos produtos.

Componentes: `LeitorPlanilhaOperacoes`, `GeradorXmlPedidos`, `EnviOperacaoRessarcimento` (JAXB).

### 4.4 Compartilhado

- **`ResultadoImportacaoDTO`** – totais e erros por linha na importação.
- **`GlobalExceptionHandler`** – validação de request (`MethodArgumentNotValidException`), `ConstraintViolationException`, declarante não encontrado, `ErroImportacaoPlanilhaException`, `IllegalArgumentException`.
- **`index.html`** (estático) – página simples listando os endpoints principais.

---

## 5. Testes automatizados

| Teste | Escopo |
|-------|--------|
| `DeclaranteServiceTest` | Serviço declarante |
| `DeclaranteApiIntegrationTest` | API declarante (integração) |
| `ProdutoServiceTest` | Importação/listagem produtos (mocks) |
| `PedidoServiceTest` | Importação pedidos (mocks) |
| `RessarcimentoApplicationIntegrationTest` | Contexto Spring (H2) |
| `RessarcimentoSqlServerIntegrationTest` | Integração com SQL Server via Testcontainers |

Recursos de teste: `application-test.yml`, scripts SQL em `src/test/resources` quando aplicável.

---

## 6. Estado do “plano” de entrega

Tudo abaixo está **concluído** no código atual:

1. Projeto Spring Boot com persistência SQL Server e Flyway V1.  
2. CRUD lógico do declarante (um registro ativo por instalação, identificado por raiz CNPJ).  
3. Importação e validação de planilhas de produtos e operações.  
4. Persistência de matriz de produtos, notas e itens.  
5. Geração e download de XMLs (produtos e pedidos por período).  
6. Histórico com armazenamento do conteúdo XML e download por id.  
7. Documentação OpenAPI + página `index.html`.  
8. Camada de exceções HTTP padronizada (`ErrorResponse`).  
9. Testes unitários e de integração (incl. opção SQL Server em container).

---

## 7. Sugestões de evolução (não implementadas)

Ideias para próximas iterações (priorizar conforme necessidade fiscal/operacional):

- **Segurança:** autenticação (OAuth2/JWT ou Basic), HTTPS, remoção de credenciais do `application.yml`.
- **Multi-declarante:** hoje várias operações assumem “primeiro declarante” ou filtram por declarante logado; evoluir para seleção explícita ou tenancy.
- **Substituição/atualização de dados:** política clara para reimportação (upsert produtos, conflito de chaves NF-e, limpeza por período).
- **Validação fiscal adicional:** cruzamento chave NF-e com schema oficial, regras do manual SEFAZ/AM por versão do layout.
- **Frontend:** UI além da página estática (upload, filtros, preview de erros).
- **Assinatura / envio DT-e:** integração com serviço de transmissão (fora do escopo atual).
- **Observabilidade:** métricas, correlation id, auditoria de importações.

---

## 8. Como acompanhar

- Manter este **`PLANO.md`** atualizado a cada marco.
- Endpoints resumidos em `src/main/resources/static/index.html`.
- Contrato detalhado em **Swagger UI** após subir a aplicação.

---

*Artefato gerado com base na análise do repositório; ajuste datas e itens de evolução conforme o roadmap da equipe.*
