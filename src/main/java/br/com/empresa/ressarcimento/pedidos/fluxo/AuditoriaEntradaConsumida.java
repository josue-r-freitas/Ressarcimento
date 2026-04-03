package br.com.empresa.ressarcimento.pedidos.fluxo;

import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditoria_entrada_consumida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEntradaConsumida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditoria_produto_id", nullable = false)
    private AuditoriaProdutoVendido auditoriaProduto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "processamento_ressarcimento_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_aud_ent_cons_proc_ress"))
    private ProcessamentoRessarcimento processamentoRessarcimento;

    @Column(name = "chave_nfe_entrada", nullable = false, columnDefinition = "CHAR(44)")
    private String chaveNfeEntrada;

    @Column(name = "seq_item", nullable = false)
    private Integer seqItem;

    @Column(name = "codg_item", length = 60)
    private String codgItem;

    @Column(name = "cnpj_fornecedor", columnDefinition = "CHAR(14)")
    private String cnpjFornecedor;

    @Column(name = "tributo", length = 10)
    private String tributo;

    @Column(name = "data_apresentacao")
    private LocalDate dataApresentacao;

    @Column(name = "quantidade_original_unidade_fornecedor", precision = 15, scale = 6)
    private BigDecimal quantidadeOriginalUnidadeFornecedor;

    @Column(name = "unidade_fornecedor", length = 8)
    private String unidadeFornecedor;

    @Column(name = "fator_conversao_aplicado", precision = 15, scale = 6)
    private BigDecimal fatorConversaoAplicado;

    @Column(name = "quantidade_convertida_unidade_interna", precision = 15, scale = 6)
    private BigDecimal quantidadeConvertidaUnidadeInterna;

    @Column(name = "conversao_aplicada", nullable = false)
    private boolean conversaoAplicada;

    @Column(name = "quantidade_consumida", precision = 15, scale = 6)
    private BigDecimal quantidadeConsumida;
}
