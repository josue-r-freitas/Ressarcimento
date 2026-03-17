package br.com.empresa.ressarcimento.shared.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoImportacaoDTO {

    private int totalLinhasProcessadas;
    private int totalLinhasComErro;
    private int totalPersistidas;
    private List<ErroPlanilhaDTO> erros;
}
