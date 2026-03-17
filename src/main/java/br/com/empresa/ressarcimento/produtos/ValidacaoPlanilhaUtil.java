package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.planilhas.dto.ProdutoPlanilhaDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class ValidacaoPlanilhaUtil {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    static List<String> validar(ProdutoPlanilhaDTO dto) {
        Set<ConstraintViolation<ProdutoPlanilhaDTO>> violations = VALIDATOR.validate(dto);
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());
    }
}
