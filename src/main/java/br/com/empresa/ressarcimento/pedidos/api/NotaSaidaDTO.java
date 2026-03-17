package br.com.empresa.ressarcimento.pedidos.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaSaidaDTO {

    private Long id;
    private String chaveNFe;
    private String anoPeriodoReferencia;
    private String mesPeriodoReferencia;
    private int quantidadeItens;
}
