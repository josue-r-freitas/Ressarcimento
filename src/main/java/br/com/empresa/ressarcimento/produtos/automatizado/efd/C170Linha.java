package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.math.BigDecimal;

public record C170Linha(int numItem, String codItem, String unid, BigDecimal qtd) {

    public C170Linha(int numItem, String codItem, String unid) {
        this(numItem, codItem, unid, null);
    }
}
