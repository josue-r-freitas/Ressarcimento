package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfoItemSped {
    private String descrItem;
    /** UNID_INV do registro 0200 (unidade de inventário interna). */
    private String unidInv;
    /** Fator do 0220; null se ausente (aplicar 1,000000). */
    private BigDecimal fatorConversao0220;
}
