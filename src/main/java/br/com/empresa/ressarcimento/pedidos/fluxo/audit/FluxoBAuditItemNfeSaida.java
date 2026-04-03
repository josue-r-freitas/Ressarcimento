package br.com.empresa.ressarcimento.pedidos.fluxo.audit;

import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fluxo_b_audit_item_nfe_saida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FluxoBAuditItemNfeSaida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_nfe_saida_id", nullable = false)
    private FluxoBAuditNfeSaida auditNfeSaida;

    @Column(name = "num_item_nfe", nullable = false)
    private Integer numItemNFe;

    @Column(name = "c_prod", nullable = false, length = 60)
    private String cProd;

    @Column(name = "cfop", nullable = false, columnDefinition = "CHAR(4)")
    private String cfop;

    @Column(name = "q_com", nullable = false, precision = 15, scale = 6)
    private BigDecimal qCom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_matriz_id")
    private ProdutoMatriz produtoMatriz;

    @Column(name = "cod_interno_resolvido", length = 60)
    private String codInternoResolvido;
}
