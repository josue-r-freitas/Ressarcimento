package br.com.empresa.ressarcimento.planilhas.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoPlanilhaDTO {

    private int numeroLinha;

    @NotBlank(message = "Código interno do produto é obrigatório")
    @Size(min = 1, max = 60)
    private String codInternoProduto;

    @NotBlank(message = "Descrição do produto é obrigatória")
    @Size(min = 1, max = 100)
    private String descricaoProduto;

    @NotBlank(message = "Unidade interna é obrigatória")
    @Size(min = 2, max = 8)
    private String unidadeInternaProduto;

    @NotNull(message = "Fator de conversão é obrigatório")
    @Digits(integer = 9, fraction = 6)
    @DecimalMax("999999999.999999")
    private BigDecimal fatorConversao;

    @NotBlank(message = "CNPJ do fornecedor é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ do fornecedor deve conter exatamente 14 dígitos")
    private String cnpjFornecedor;

    @NotBlank(message = "Código do produto no fornecedor é obrigatório")
    @Size(min = 1, max = 60)
    private String codProdFornecedor;

    @NotBlank(message = "Unidade do produto no fornecedor é obrigatória")
    @Size(min = 2, max = 8)
    private String unidadeProdutoFornecedor;
}
