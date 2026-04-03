package br.com.empresa.ressarcimento.processamento;

import br.com.empresa.ressarcimento.pedidos.api.GerarPedidoAutomaticoResponse;
import br.com.empresa.ressarcimento.pedidos.fluxo.FluxoPedidoAutomaticoService;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import br.com.empresa.ressarcimento.produtos.ProdutoService;
import br.com.empresa.ressarcimento.produtos.api.GerarPlanilhaAutomaticaRequest;
import br.com.empresa.ressarcimento.produtos.api.ResultadoGeracaoPlanilhaAutomatica;
import br.com.empresa.ressarcimento.produtos.automatizado.ProdutoPlanilhaAutomaticaService;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessamentoRessarcimentoService {

    private static final int MENSAGEM_ERRO_MAX = 2000;

    private final ProcessamentoRessarcimentoRepository processamentoRepository;
    private final ProcessamentoRessarcimentoLifecycle processamentoRessarcimentoLifecycle;
    private final ProdutoPlanilhaAutomaticaService produtoPlanilhaAutomaticaService;
    private final ProdutoService produtoService;
    private final FluxoPedidoAutomaticoService fluxoPedidoAutomaticoService;

    @Transactional
    public ProcessamentoRessarcimento iniciar(int ano, int mes) {
        return processamentoRessarcimentoLifecycle.iniciarEmAndamento(ano, mes);
    }

    @Transactional
    public void marcarConcluido(Long processamentoRessarcimentoId) {
        ProcessamentoRessarcimento p = processamentoRepository
                .findById(processamentoRessarcimentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Processamento de ressarcimento não encontrado: " + processamentoRessarcimentoId));
        p.setDataHoraFim(LocalDateTime.now());
        p.setStatusExecucao(ProcessamentoRessarcimento.STATUS_CONCLUIDO);
        p.setMensagemErro(null);
        processamentoRepository.save(p);
    }

    @Transactional
    public void marcarErro(Long processamentoRessarcimentoId, String mensagem) {
        ProcessamentoRessarcimento p = processamentoRepository
                .findById(processamentoRessarcimentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Processamento de ressarcimento não encontrado: " + processamentoRessarcimentoId));
        p.setDataHoraFim(LocalDateTime.now());
        p.setStatusExecucao(ProcessamentoRessarcimento.STATUS_ERRO);
        p.setMensagemErro(truncarMensagem(mensagem));
        processamentoRepository.save(p);
    }

    /**
     * Orquestra planilha automática de produtos, importação da matriz, Fluxo B de pedidos e geração do XML de produtos,
     * com rastreio em {@link ProcessamentoRessarcimento} e FKs nas entidades envolvidas.
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultadoPipelineProcessamento executarPipelineCompleto(int ano, int mes, GerarPlanilhaAutomaticaRequest planilhaReq)
            throws IOException, JAXBException {
        ProcessamentoRessarcimento proc = iniciar(ano, mes);
        Long pid = proc.getId();
        try {
            ResultadoGeracaoPlanilhaAutomatica planilha =
                    produtoPlanilhaAutomaticaService.gerarPlanilhaAutomatica(planilhaReq, pid);
            byte[] xlsx = planilha.getPlanilhaXlsx();
            if (xlsx == null || xlsx.length == 0) {
                throw new IllegalStateException("Geração da planilha automática não produziu conteúdo XLSX.");
            }
            try (ByteArrayInputStream in = new ByteArrayInputStream(xlsx)) {
                ResultadoImportacaoDTO imp = produtoService.importar(in, "planilha_produtos.xlsx", pid);
                if (imp.getTotalLinhasComErro() > 0) {
                    throw new IllegalStateException(
                            "Importação da matriz de produtos falhou: " + imp.getTotalLinhasComErro() + " linha(s) com erro.");
                }
            }
            GerarPedidoAutomaticoResponse pedidoResp =
                    fluxoPedidoAutomaticoService.gerarAutomatico(ano, mes, pid);
            Long arquivoProdutosId = produtoService.gerarXmlRetornandoIdArquivo(pid);
            marcarConcluido(pid);
            return ResultadoPipelineProcessamento.builder()
                    .processamentoRessarcimentoId(pid)
                    .respostaPedidos(pedidoResp)
                    .arquivoProdutosId(arquivoProdutosId)
                    .build();
        } catch (Exception e) {
            marcarErro(pid, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            throw e;
        }
    }

    private static String truncarMensagem(String mensagem) {
        if (mensagem == null) {
            return null;
        }
        if (mensagem.length() <= MENSAGEM_ERRO_MAX) {
            return mensagem;
        }
        return mensagem.substring(0, MENSAGEM_ERRO_MAX - 3) + "...";
    }

    @Getter
    @Builder
    public static class ResultadoPipelineProcessamento {
        private final Long processamentoRessarcimentoId;
        private final GerarPedidoAutomaticoResponse respostaPedidos;
        private final Long arquivoProdutosId;
    }
}
