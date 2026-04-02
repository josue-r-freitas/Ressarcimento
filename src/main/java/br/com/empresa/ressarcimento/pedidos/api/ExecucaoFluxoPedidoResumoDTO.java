package br.com.empresa.ressarcimento.pedidos.api;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecucaoFluxoPedidoResumoDTO {

    private Long id;
    private String anoReferencia;
    private String mesReferencia;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private String statusExecucao;
}
