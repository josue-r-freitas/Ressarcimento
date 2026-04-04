package br.com.empresa.ressarcimento.pedidos.api;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RastreabilidadeFluxoDTO {

    private ExecucaoFluxoPedidoResumoDTO processamento;
    private List<AuditoriaProdutoVendidoDTO> produtosVendidos;
    private List<Map<String, String>> logs;
}
