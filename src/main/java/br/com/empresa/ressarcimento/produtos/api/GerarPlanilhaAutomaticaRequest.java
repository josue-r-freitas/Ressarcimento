package br.com.empresa.ressarcimento.produtos.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GerarPlanilhaAutomaticaRequest {

    private Integer anoReferencia;
    private Integer mesReferencia;
    /** Nome do arquivo .xlsx dentro da pasta configurada (ex.: resumonf.xlsx). */
    private String nomeArquivoResumo;
}
