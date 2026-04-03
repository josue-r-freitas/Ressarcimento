package br.com.empresa.ressarcimento.pedidos.domain;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nota_saida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaSaida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declarante_id", nullable = false)
    private Declarante declarante;

    @Column(name = "chave_nfe", nullable = false, columnDefinition = "CHAR(44)")
    private String chaveNFe;

    @Column(name = "ano_periodo_referencia", nullable = false, columnDefinition = "CHAR(4)")
    private String anoPeriodoReferencia;

    @Column(name = "mes_periodo_referencia", nullable = false, columnDefinition = "CHAR(2)")
    private String mesPeriodoReferencia;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processamento_ressarcimento_id")
    private ProcessamentoRessarcimento processamentoRessarcimento;

    @OneToMany(mappedBy = "notaSaida", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemNotaSaida> itens = new ArrayList<>();

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
