package br.com.empresa.ressarcimento.produtos.api;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArquivoProdutosDTO {

    private Long id;
    private LocalDateTime dataGeracao;
    private String status;
    private String mensagemLog;
}
