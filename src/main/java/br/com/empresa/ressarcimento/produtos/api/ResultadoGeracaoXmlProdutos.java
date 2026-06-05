package br.com.empresa.ressarcimento.produtos.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoGeracaoXmlProdutos {

    private Long arquivoProdutosId;
    private List<String> codigosIncluidosNoXml;
    private List<String> codigosSemMatriz;
}
