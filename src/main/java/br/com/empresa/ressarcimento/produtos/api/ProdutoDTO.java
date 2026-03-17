package br.com.empresa.ressarcimento.produtos.api;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoDTO {

    private Long id;
    private String codInternoProduto;
    private String descricaoProduto;
    private String unidadeInternaProduto;
    private BigDecimal fatorConversao;
    private String cnpjFornecedor;
    private String codProdFornecedor;
    private String unidadeProdutoFornecedor;
}
