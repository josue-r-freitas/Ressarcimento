package br.com.empresa.ressarcimento.pedidos.domain;

import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nota_entrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_nfe_entrada", nullable = false, columnDefinition = "CHAR(44)")
    private String chaveNFeEntrada;

    @Column(name = "chave_cte_entrada", columnDefinition = "CHAR(44)")
    private String chaveCTeEntrada;

    @Column(name = "chave_mdfe_entrada", columnDefinition = "CHAR(44)")
    private String chaveMDFeEntrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processamento_ressarcimento_id", nullable = false)
    private ProcessamentoRessarcimento processamentoRessarcimento;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
