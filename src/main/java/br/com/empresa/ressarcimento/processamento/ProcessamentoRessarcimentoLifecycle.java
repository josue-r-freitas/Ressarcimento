package br.com.empresa.ressarcimento.processamento;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criação de cabeçalhos {@link ProcessamentoRessarcimento} sem depender de serviços de domínio que referenciam o
 * pipeline completo (evita ciclos de injeção).
 */
@Service
@RequiredArgsConstructor
public class ProcessamentoRessarcimentoLifecycle {

    private final ProcessamentoRessarcimentoRepository processamentoRepository;
    private final DeclaranteService declaranteService;

    @Transactional
    public ProcessamentoRessarcimento iniciarEmAndamento(int ano, int mes) {
        String anoStr = String.format("%04d", ano);
        String mesStr = mes >= 1 && mes <= 9 ? "0" + mes : String.valueOf(mes);
        ProcessamentoRessarcimento p = ProcessamentoRessarcimento.builder()
                .declarante(declaranteService.getEntidadeOuLanca())
                .anoReferencia(anoStr)
                .mesReferencia(mesStr)
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO)
                .build();
        return processamentoRepository.save(p);
    }
}
