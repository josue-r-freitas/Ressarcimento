package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.pedidos.domain.ItemNotaSaida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemNotaSaidaRepository extends JpaRepository<ItemNotaSaida, Long> {

    /** Necessário antes de apagar {@code produto_matriz} (FK em {@code item_nota_saida}). */
    @Modifying
    @Query("UPDATE ItemNotaSaida i SET i.produtoMatriz = null WHERE i.produtoMatriz IS NOT NULL")
    int desvincularProdutosMatriz();

    /** Códigos internos distintos presentes em itens de NF-e de saída do declarante (via {@code nota_saida}). */
    @Query(
            "SELECT DISTINCT i.codInternoProduto FROM ItemNotaSaida i JOIN i.notaSaida n WHERE n.declarante.id = :declaranteId")
    List<String> findDistinctCodInternoProdutoByNotaSaidaDeclaranteId(@Param("declaranteId") Long declaranteId);

    /**
     * Mesmo critério que {@link #findDistinctCodInternoProdutoByNotaSaidaDeclaranteId}, restrito a notas de saída
     * vinculadas ao processamento (ex.: pipeline «Processar Ressarcimento»).
     */
    @Query(
            "SELECT DISTINCT i.codInternoProduto FROM ItemNotaSaida i JOIN i.notaSaida n "
                    + "WHERE n.declarante.id = :declaranteId AND n.processamentoRessarcimento.id = :processamentoId")
    List<String> findDistinctCodInternoProdutoByNotaSaidaDeclaranteIdAndProcessamentoId(
            @Param("declaranteId") Long declaranteId, @Param("processamentoId") Long processamentoId);
}
