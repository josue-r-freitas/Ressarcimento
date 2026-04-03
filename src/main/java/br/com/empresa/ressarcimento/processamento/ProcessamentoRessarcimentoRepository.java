package br.com.empresa.ressarcimento.processamento;

import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessamentoRessarcimentoRepository extends JpaRepository<ProcessamentoRessarcimento, Long> {}
