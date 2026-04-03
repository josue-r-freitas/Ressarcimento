package br.com.empresa.ressarcimento.pedidos.fluxo.audit;

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
@Table(name = "fluxo_b_audit_item_nfe_entrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FluxoBAuditItemNfeEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_nfe_entrada_id", nullable = false)
    private FluxoBAuditNfeEntrada auditNfeEntrada;

    @Column(name = "seq_item", nullable = false)
    private Integer seqItem;

    @Column(name = "codg_item", length = 60)
    private String codgItem;

    @Column(name = "tributo", length = 20)
    private String tributo;

    @Column(name = "qtd_unit_compra", precision = 15, scale = 6)
    private BigDecimal qtdUnitCompra;

    @Column(name = "valor_unitario", precision = 15, scale = 6)
    private BigDecimal valorUnitario;

    @Column(name = "cfop", columnDefinition = "CHAR(4)")
    private String cfop;

    @Column(name = "valor_imposto", precision = 15, scale = 6)
    private BigDecimal valorImposto;

    @Column(name = "cnpj_fornecedor", columnDefinition = "CHAR(14)")
    private String cnpjFornecedor;

    @Column(name = "numero_linha_planilha")
    private Integer numeroLinhaPlanilha;
}
