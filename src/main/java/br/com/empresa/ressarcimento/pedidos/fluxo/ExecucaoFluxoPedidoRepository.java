package br.com.empresa.ressarcimento.pedidos.fluxo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExecucaoFluxoPedidoRepository extends JpaRepository<ExecucaoFluxoPedido, Long> {

    Page<ExecucaoFluxoPedido> findByDeclaranteIdOrderByDataHoraInicioDesc(Long declaranteId, Pageable pageable);

    @EntityGraph(attributePaths = {"auditoriasProduto", "auditoriasProduto.entradasConsumidas", "logs"})
    @Query("select e from ExecucaoFluxoPedido e where e.id = :id")
    Optional<ExecucaoFluxoPedido> findDetailedById(@Param("id") Long id);
}
