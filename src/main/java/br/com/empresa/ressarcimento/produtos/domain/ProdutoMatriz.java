package br.com.empresa.ressarcimento.produtos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "produto_matriz")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoMatriz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** codInternoProduto (D03) 1-60 */
    @Column(name = "cod_interno_produto", nullable = false, length = 60)
    @NotBlank
    @Size(min = 1, max = 60)
    private String codInternoProduto;

    /** descricaoProduto (D04) 1-100 */
    @Column(name = "descricao_produto", nullable = false, length = 100)
    @NotBlank
    @Size(min = 1, max = 100)
    private String descricaoProduto;

    /** unidadeInternaProduto (D05) TUnidProduto: 1–6 (XSD tiposBasicos) */
    @Column(name = "unidade_interna_produto", nullable = false, length = 8)
    @NotBlank
    @Size(min = 1, max = 6)
    private String unidadeInternaProduto;

    /** fatorConversao (D06) 1-9 dígitos, 6 decimais */
    @Column(name = "fator_conversao", nullable = false, precision = 15, scale = 6)
    @NotNull
    @Digits(integer = 9, fraction = 6)
    @DecimalMax(value = "999999999.999999")
    private BigDecimal fatorConversao;

    /** cnpjFornecedor (D07) 14 dígitos */
    @Column(name = "cnpj_fornecedor", nullable = false, columnDefinition = "CHAR(14)")
    @NotBlank
    @Pattern(regexp = "\\d{14}", message = "CNPJ do fornecedor deve conter exatamente 14 dígitos")
    private String cnpjFornecedor;

    /** codProdFornecedor (D08) 1-60 */
    @Column(name = "cod_prod_fornecedor", nullable = false, length = 60)
    @NotBlank
    @Size(min = 1, max = 60)
    private String codProdFornecedor;

    /** unidadeProdutoFornecedor (D09); valores vindos da NF-e uCom: 1–6 caracteres (manual NF-e). */
    @Column(name = "unidade_produto_fornecedor", nullable = false, length = 8)
    @NotBlank
    @Size(min = 1, max = 6)
    private String unidadeProdutoFornecedor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
