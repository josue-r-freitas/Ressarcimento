package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.planilhas.dto.OperacaoPlanilhaDTO;
import br.com.empresa.ressarcimento.produtos.ProdutoMatrizRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidacaoPlanilhaOperacoes {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private final ProdutoMatrizRepository produtoRepository;

    public List<String> validar(OperacaoPlanilhaDTO dto) {
        List<String> erros = new ArrayList<>();
        Set<ConstraintViolation<OperacaoPlanilhaDTO>> violations = VALIDATOR.validate(dto);
        erros.addAll(violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList()));
        if (dto.getCodInternoProduto() != null && !dto.getCodInternoProduto().isBlank()) {
            if (!produtoRepository.existsByCodInternoProduto(dto.getCodInternoProduto())) {
                erros.add("codInternoProduto: Produto não cadastrado na matriz. Cadastre o produto antes de importar operações.");
            }
        }
        try {
            int num = Integer.parseInt(dto.getNumItemNfe() != null ? dto.getNumItemNfe().trim() : "0");
            if (num < 1 || num > 999) {
                erros.add("numItemNFe: Deve ser entre 1 e 999");
            }
        } catch (NumberFormatException e) {
            erros.add("numItemNFe: Valor numérico inválido");
        }
        return erros;
    }
}
