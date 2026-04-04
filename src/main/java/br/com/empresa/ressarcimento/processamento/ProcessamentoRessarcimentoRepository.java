package br.com.empresa.ressarcimento.processamento;

import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessamentoRessarcimentoRepository extends JpaRepository<ProcessamentoRessarcimento, Long> {

    List<ProcessamentoRessarcimento> findByDeclaranteIdOrderByDataHoraInicioDesc(Long declaranteId, Pageable pageable);

    Page<ProcessamentoRessarcimento> findByDeclaranteIdAndArquivoEfdUtilizadoIsNotNullOrderByDataHoraInicioDesc(
            Long declaranteId, Pageable pageable);

    @EntityGraph(attributePaths = {
        "auditoriasProdutoFluxoB",
        "auditoriasProdutoFluxoB.entradasConsumidas",
        "logsFluxoPedido"
    })
    @Query("select p from ProcessamentoRessarcimento p where p.id = :id")
    Optional<ProcessamentoRessarcimento> findDetailedFluxoBById(@Param("id") Long id);
}
