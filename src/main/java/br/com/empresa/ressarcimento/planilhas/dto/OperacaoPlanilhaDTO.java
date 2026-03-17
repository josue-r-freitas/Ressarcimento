package br.com.empresa.ressarcimento.planilhas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperacaoPlanilhaDTO {

    private int numeroLinha;

    @NotBlank(message = "Ano de referência é obrigatório")
    @Pattern(regexp = "\\d{4}", message = "Ano deve ter 4 dígitos")
    private String anoReferencia;

    @NotBlank(message = "Mês de referência é obrigatório")
    @Pattern(regexp = "0[1-9]|1[0-2]", message = "Mês deve ser entre 01 e 12")
    private String mesReferencia;

    @NotBlank(message = "Chave da NF-e de saída é obrigatória")
    @Pattern(regexp = "\\d{44}", message = "Chave NF-e deve ter exatamente 44 dígitos")
    private String chaveNfeSaida;

    @NotBlank(message = "Número do item na NF-e é obrigatório")
    private String numItemNfe;

    @NotBlank(message = "Código interno do produto é obrigatório")
    @Size(min = 1, max = 60)
    private String codInternoProduto;

    /** Opcional - 44 dígitos se preenchido */
    @Pattern(regexp = "|\\d{44}", message = "Chave NF-e entrada deve ter 44 dígitos ou estar vazia")
    private String chaveNfeEntrada;

    @Pattern(regexp = "|\\d{44}", message = "Chave CT-e entrada deve ter 44 dígitos ou estar vazia")
    private String chaveCteEntrada;

    @Pattern(regexp = "|\\d{44}", message = "Chave MDF-e entrada deve ter 44 dígitos ou estar vazia")
    private String chaveMdfeEntrada;
}
