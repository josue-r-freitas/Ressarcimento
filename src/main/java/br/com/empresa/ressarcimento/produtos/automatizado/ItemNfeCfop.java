package br.com.empresa.ressarcimento.produtos.automatizado;

import java.math.BigDecimal;

/** Item da NF-e de saída relevante ao Fluxo B (CFOP filtrado no XML). */
public record ItemNfeCfop(int nItem, String cfop, String cProd, BigDecimal qCom, String uCom) {}
