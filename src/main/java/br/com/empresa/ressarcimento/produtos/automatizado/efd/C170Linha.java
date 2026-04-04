package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.math.BigDecimal;

/**
 * Item C170 da EFD (NF-e entrada/saída). Campos opcionais vêm do layout SPED ICMS/IPI após QTD/UNID.
 */
public record C170Linha(
        int numItem,
        String codItem,
        String unid,
        BigDecimal qtd,
        BigDecimal vlUnit,
        String cfop,
        BigDecimal vlIcms) {

    public C170Linha(int numItem, String codItem, String unid, BigDecimal qtd) {
        this(numItem, codItem, unid, qtd, null, null, null);
    }
}
