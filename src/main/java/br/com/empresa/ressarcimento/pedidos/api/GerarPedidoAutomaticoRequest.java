package br.com.empresa.ressarcimento.pedidos.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GerarPedidoAutomaticoRequest {

    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer ano;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer mes;
}
