package br.com.empresa.ressarcimento.pedidos.domain;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_nota_saida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemNotaSaida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_saida_id", nullable = false)
    private NotaSaida notaSaida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_matriz_id")
    private ProdutoMatriz produtoMatriz;

    @Column(name = "cod_interno_produto", nullable = false, length = 60)
    private String codInternoProduto;

    @Column(name = "num_item_nfe", nullable = false)
    private Integer numItemNFe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_entrada_id")
    private NotaEntrada notaEntrada;
}
