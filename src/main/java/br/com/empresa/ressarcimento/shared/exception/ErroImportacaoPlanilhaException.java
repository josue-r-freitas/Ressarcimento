package br.com.empresa.ressarcimento.shared.exception;

import br.com.empresa.ressarcimento.shared.api.ErroPlanilhaDTO;
import java.util.List;
import lombok.Getter;

@Getter
public class ErroImportacaoPlanilhaException extends RuntimeException {

    private final List<ErroPlanilhaDTO> erros;

    public ErroImportacaoPlanilhaException(List<ErroPlanilhaDTO> erros) {
        super("A planilha contém erros de validação. Corrija os dados e tente novamente.");
        this.erros = erros;
    }
}
