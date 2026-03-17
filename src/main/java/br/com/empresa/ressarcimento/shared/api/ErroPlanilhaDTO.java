package br.com.empresa.ressarcimento.shared.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErroPlanilhaDTO {

    private int linha;
    private String campo;
    private String valorInformado;
    private String mensagem;
}
