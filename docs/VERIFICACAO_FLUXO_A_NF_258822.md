# Verificação Fluxo A — NF 258822 (chave …80832321)

Documento de referência para a NF-e de entrada **35250621009346000120550100002588221980832321** no Fluxo A (`/ui/produtos/planilha-automatica`).

## Dados no resumonf.xlsx (linha 674)

| Campo | Valor |
|-------|-------|
| TRIBUTO | 1380 |
| CHAVE | 35250621009346000120550100002588221980832321 |
| NR. NOTA | 258822 |
| SEQ. ITEM | 1 |
| CODG. ITEM | 56234 |
| CNPJ FORNECEDOR | 21.009.346/0001-20 |
| DATA APRES. | 20/06/2025 |

## Pipeline após leitura do resumo

1. **LeitorResumoNf** — ignora linhas sem `DATA APRES.` ou sem `CODG. ITEM`; ordena por chave + seq.
2. **filtrarPorPeriodo** — com Ano=2025 e Mês=6, mantém linhas cuja `DATA APRES.` cai em junho/2025.
3. **Validações obrigatórias** (rejeitam a linha): chave 44 dígitos, seq > 0, CNPJ 14 dígitos, CODG preenchido, NF entrada no EFD (C100 `IND_OPER=0`, `MOD=55`), C170 com `NUM_ITEM` = seq, 0200 com descrição para `COD_ITEM` do C170.
4. **Enriquecimento** (não rejeita): unidade interna (0200/C170), fator 0220 ou 1,0, XML de entrada e `uCom` (`nItem` + `cProd` = CODG quando informado).
5. **Montagem** — `cod_interno_produto` vem do **C170**, não do resumo; `cod_prod_fornecedor` vem do **CODG. ITEM** do resumo.
6. **Deduplicação** — chave `codInterno|cnpj|codFornecedor|unidadeInterna|unidadeFornecedor`.

## Resultado esperado (EFD com C170 = 56234)

| Campo planilha | Valor |
|----------------|-------|
| cod_interno_produto | 56234 |
| cod_prod_fornecedor | 56234 |
| cnpj_fornecedor | 21009346000120 |
| unidade_interna / fornecedor | UN / UN |

**Nota:** se o C170 da EFD em produção usar outro `COD_ITEM` (ex.: 10564), o código interno na planilha seguirá o EFD, não o resumo.

## Testes automatizados

- [`FluxoANf258822Fixtures`](../src/test/java/br/com/empresa/ressarcimento/produtos/automatizado/FluxoANf258822Fixtures.java) — fixture da linha 674
- [`ProdutoPlanilhaAutomaticaNf258822Test`](../src/test/java/br/com/empresa/ressarcimento/produtos/automatizado/ProdutoPlanilhaAutomaticaNf258822Test.java) — leitura do resumo e geração da planilha

Executar:

```bash
mvn test -Dtest=ProdutoPlanilhaAutomaticaNf258822Test
```

## Validação manual

1. Gerar planilha em `/ui/produtos/planilha-automatica` (2025, 6, `resumonf.xlsx`).
2. Conferir cabeçalhos `X-Ressarcimento-*` (linhas resumo / rejeitadas).
3. Em `/ui/produtos/logs-geracao`, a chave não deve ter `NOTA_NAO_ENCONTRADA_EFD` nem `ITEM_NAO_ENCONTRADO_NO_EFD`.
4. Na planilha baixada, buscar **56234** (ou o `COD_ITEM` efetivo do C170 na EFD carregada).
