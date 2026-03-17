package br.com.empresa.ressarcimento.declarante.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "declarante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Declarante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** cnpjRaiz (C02) – 8 dígitos numéricos */
    @Column(name = "cnpj_raiz", nullable = false, columnDefinition = "CHAR(8)")
    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "CNPJ raiz deve conter exatamente 8 dígitos numéricos")
    private String cnpjRaiz;

    /** ieContribuinteDeclarante (C03) – 8 a 15 dígitos */
    @Column(name = "ie_contribuinte", nullable = false, length = 15)
    @NotBlank
    @Pattern(regexp = "\\d{8,15}", message = "IE deve conter entre 8 e 15 dígitos numéricos")
    private String ieContribuinteDeclarante;

    @Column(name = "razao_social", nullable = false, length = 60)
    @NotBlank
    @Size(min = 3, max = 60)
    private String razaoSocial;

    @Column(name = "nome_responsavel", nullable = false, length = 60)
    @NotBlank
    @Size(min = 3, max = 60)
    private String nomeResponsavel;

    @Column(name = "fone_responsavel", nullable = false, length = 15)
    @NotBlank
    @Size(min = 7, max = 15)
    private String foneResponsavel;

    @Column(name = "email_responsavel", nullable = false, length = 60)
    @NotBlank
    @Size(min = 3, max = 60)
    @Email
    private String emailResponsavel;

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
