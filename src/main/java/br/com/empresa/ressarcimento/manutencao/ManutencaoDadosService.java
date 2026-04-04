package br.com.empresa.ressarcimento.manutencao;

import br.com.empresa.ressarcimento.pedidos.ArquivoPedidoRepository;
import br.com.empresa.ressarcimento.pedidos.ItemNotaSaidaRepository;
import br.com.empresa.ressarcimento.pedidos.NotaEntradaRepository;
import br.com.empresa.ressarcimento.pedidos.NotaSaidaRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.AuditoriaEntradaConsumidaRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.AuditoriaProdutoVendidoRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.LogExecucaoFluxoRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditItemNfeEntradaRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditItemNfeSaidaRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditNfeEntradaRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditNfeSaidaRepository;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoRepository;
import br.com.empresa.ressarcimento.produtos.ArquivoProdutosRepository;
import br.com.empresa.ressarcimento.produtos.ProdutoMatrizRepository;
import br.com.empresa.ressarcimento.produtos.automatizado.LogGeracaoPlanilhaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remove dados operacionais mantendo apenas {@link br.com.empresa.ressarcimento.declarante.domain.Declarante}.
 */
@Service
@RequiredArgsConstructor
public class ManutencaoDadosService {

    private final FluxoBAuditItemNfeSaidaRepository fluxoBAuditItemNfeSaidaRepository;
    private final FluxoBAuditNfeSaidaRepository fluxoBAuditNfeSaidaRepository;
    private final FluxoBAuditItemNfeEntradaRepository fluxoBAuditItemNfeEntradaRepository;
    private final FluxoBAuditNfeEntradaRepository fluxoBAuditNfeEntradaRepository;
    private final ItemNotaSaidaRepository itemNotaSaidaRepository;
    private final ArquivoPedidoRepository arquivoPedidoRepository;
    private final ArquivoProdutosRepository arquivoProdutosRepository;
    private final LogGeracaoPlanilhaRepository logGeracaoPlanilhaRepository;
    private final NotaSaidaRepository notaSaidaRepository;
    private final AuditoriaEntradaConsumidaRepository auditoriaEntradaConsumidaRepository;
    private final AuditoriaProdutoVendidoRepository auditoriaProdutoVendidoRepository;
    private final LogExecucaoFluxoRepository logExecucaoFluxoRepository;
    private final ProcessamentoRessarcimentoRepository processamentoRessarcimentoRepository;
    private final NotaEntradaRepository notaEntradaRepository;
    private final ProdutoMatrizRepository produtoMatrizRepository;
    private final EntityManager entityManager;

    @Transactional
    public void limparTudoExcetoDeclarante() {
        fluxoBAuditItemNfeSaidaRepository.deleteAllInBatch();
        fluxoBAuditNfeSaidaRepository.deleteAllInBatch();
        fluxoBAuditItemNfeEntradaRepository.deleteAllInBatch();
        fluxoBAuditNfeEntradaRepository.deleteAllInBatch();
        entityManager.flush();

        itemNotaSaidaRepository.deleteAllInBatch();
        entityManager.flush();

        arquivoPedidoRepository.deleteAllInBatch();
        arquivoProdutosRepository.deleteAllInBatch();
        logGeracaoPlanilhaRepository.deleteAllInBatch();
        entityManager.flush();

        notaSaidaRepository.deleteAllInBatch();
        entityManager.flush();

        auditoriaEntradaConsumidaRepository.deleteAllInBatch();
        auditoriaProdutoVendidoRepository.deleteAllInBatch();
        logExecucaoFluxoRepository.deleteAllInBatch();
        entityManager.flush();

        notaEntradaRepository.deleteAllInBatch();
        entityManager.flush();

        produtoMatrizRepository.deleteAllInBatch();
        entityManager.flush();

        processamentoRessarcimentoRepository.deleteAllInBatch();
        entityManager.flush();
    }
}
