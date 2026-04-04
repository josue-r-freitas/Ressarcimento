package br.com.empresa.ressarcimento.pedidos.fluxo;

import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import jakarta.persistence.CascadeType;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria_produto_vendido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaProdutoVendido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processamento_ressarcimento_id", nullable = false)
    private ProcessamentoRessarcimento processamentoRessarcimento;

    @Column(name = "cod_interno_produto", nullable = false, length = 60)
    private String codInternoProduto;

    @Column(name = "chave_nfe_saida", nullable = false, columnDefinition = "CHAR(44)")
    private String chaveNfeSaida;

    @Column(name = "num_item_nfe", nullable = false)
    private Integer numItemNfe;

    @Column(name = "cfop_item", columnDefinition = "CHAR(4)")
    private String cfopItem;

    @Column(name = "quantidade_vendida_unidade_interna", nullable = false, precision = 15, scale = 6)
    private BigDecimal quantidadeVendidaUnidadeInterna;

    @Column(name = "unidade_interna", nullable = false, length = 8)
    private String unidadeInterna;

    @Column(name = "quantidade_total_compras_convertida", precision = 15, scale = 6)
    private BigDecimal quantidadeTotalComprasConvertida;

    @Column(name = "suficiente", nullable = false)
    private boolean suficiente;

    @OneToMany(mappedBy = "auditoriaProduto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AuditoriaEntradaConsumida> entradasConsumidas = new ArrayList<>();
}
