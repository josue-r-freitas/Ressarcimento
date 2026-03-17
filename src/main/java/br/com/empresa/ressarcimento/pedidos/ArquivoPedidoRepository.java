package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.pedidos.domain.ArquivoPedido;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArquivoPedidoRepository extends JpaRepository<ArquivoPedido, Long> {

    List<ArquivoPedido> findByDeclaranteIdOrderByDataGeracaoDesc(Long declaranteId, Pageable pageable);

    List<ArquivoPedido> findByDeclaranteIdAndAnoReferenciaAndMesReferencia(
            Long declaranteId, String ano, String mes);
}
