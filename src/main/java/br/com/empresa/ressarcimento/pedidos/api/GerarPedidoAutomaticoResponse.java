package br.com.empresa.ressarcimento.pedidos.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GerarPedidoAutomaticoResponse {

    private Long processamentoRessarcimentoId;
    private Long arquivoPedidoId;
    private String status;
    private List<String> avisos;
}
