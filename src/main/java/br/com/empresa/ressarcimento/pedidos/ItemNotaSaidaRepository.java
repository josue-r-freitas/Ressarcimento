package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.pedidos.domain.ItemNotaSaida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ItemNotaSaidaRepository extends JpaRepository<ItemNotaSaida, Long> {

    /** Necessário antes de apagar {@code produto_matriz} (FK em {@code item_nota_saida}). */
    @Modifying
    @Query("UPDATE ItemNotaSaida i SET i.produtoMatriz = null WHERE i.produtoMatriz IS NOT NULL")
    int desvincularProdutosMatriz();
}
