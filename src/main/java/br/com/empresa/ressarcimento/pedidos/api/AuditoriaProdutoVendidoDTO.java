package br.com.empresa.ressarcimento.pedidos.api;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaProdutoVendidoDTO {

    private String codInternoProduto;
    private String chaveNfeSaida;
    private Integer numItemNfe;
    private String cfopItem;
    private BigDecimal quantidadeVendidaUnidadeInterna;
    private String unidadeInterna;
    private BigDecimal quantidadeTotalComprasConvertida;
    private boolean suficiente;
    private List<AuditoriaEntradaConsumidaDTO> entradasConsumidas;
}
