package br.com.empresa.ressarcimento.processamento;

import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessamentoRessarcimentoRepository extends JpaRepository<ProcessamentoRessarcimento, Long> {

    List<ProcessamentoRessarcimento> findByDeclaranteIdOrderByDataHoraInicioDesc(Long declaranteId, Pageable pageable);
}
