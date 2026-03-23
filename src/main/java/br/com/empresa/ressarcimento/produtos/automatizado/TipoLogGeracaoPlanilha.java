package br.com.empresa.ressarcimento.produtos.automatizado;

public enum TipoLogGeracaoPlanilha {
    NOTA_NAO_ENCONTRADA_EFD,
    ITEM_NAO_ENCONTRADO_NO_EFD,
    XML_NFE_NAO_ENCONTRADO,
    DADO_INVALIDO,
    PERIODO_MISTURADO,
    /** 0220 ausente; fator 1,0 aplicado (informativo, opcional). */
    FATOR_PADRAO_SEM_0220
}
