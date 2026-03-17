package br.com.empresa.ressarcimento.declarante.api;

import jakarta.validation.constraints.Email;
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
public class DeclaranteDTO {

    private Long id;

    @NotBlank(message = "CNPJ raiz é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "CNPJ raiz deve conter exatamente 8 dígitos numéricos")
    private String cnpjRaiz;

    @NotBlank(message = "IE do contribuinte é obrigatória")
    @Pattern(regexp = "\\d{8,15}", message = "IE deve conter entre 8 e 15 dígitos numéricos")
    private String ieContribuinteDeclarante;

    @NotBlank(message = "Razão social é obrigatória")
    @Size(min = 3, max = 60)
    private String razaoSocial;

    @NotBlank(message = "Nome do responsável é obrigatório")
    @Size(min = 3, max = 60)
    private String nomeResponsavel;

    @NotBlank(message = "Telefone do responsável é obrigatório")
    @Size(min = 7, max = 15)
    private String foneResponsavel;

    @NotBlank(message = "E-mail do responsável é obrigatório")
    @Size(min = 3, max = 60)
    @Email(message = "E-mail inválido")
    private String emailResponsavel;
}
