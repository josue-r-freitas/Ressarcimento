package br.com.empresa.ressarcimento.produtos.api;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogGeracaoPlanilhaDTO {

    private Long id;
    private String tipo;
    private String chaveNfe;
    private Integer numItem;
    private LocalDateTime dataProcessamento;
    private String arquivoOrigem;
    private String mensagem;
}
