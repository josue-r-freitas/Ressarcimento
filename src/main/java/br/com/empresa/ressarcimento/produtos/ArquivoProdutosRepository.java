package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.produtos.domain.ArquivoProdutos;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArquivoProdutosRepository extends JpaRepository<ArquivoProdutos, Long> {

    List<ArquivoProdutos> findByDeclaranteIdOrderByDataGeracaoDesc(Long declaranteId, Pageable pageable);

    List<ArquivoProdutos> findByDeclaranteIdAndDataGeracaoBetweenOrderByDataGeracaoDesc(
            Long declaranteId, LocalDateTime inicio, LocalDateTime fim);
}
