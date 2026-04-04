package br.com.empresa.ressarcimento.processamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.pedidos.api.GerarPedidoAutomaticoResponse;
import br.com.empresa.ressarcimento.pedidos.fluxo.FluxoPedidoAutomaticoService;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import br.com.empresa.ressarcimento.produtos.ProdutoService;
import br.com.empresa.ressarcimento.produtos.api.GerarPlanilhaAutomaticaRequest;
import br.com.empresa.ressarcimento.produtos.api.ResultadoGeracaoPlanilhaAutomatica;
import br.com.empresa.ressarcimento.produtos.automatizado.ProdutoPlanilhaAutomaticaService;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessamentoRessarcimentoServiceTest {

    @Mock
    private ProcessamentoRessarcimentoRepository processamentoRepository;

    @Mock
    private ProcessamentoRessarcimentoLifecycle processamentoRessarcimentoLifecycle;

    @Mock
    private ProdutoPlanilhaAutomaticaService produtoPlanilhaAutomaticaService;

    @Mock
    private ProdutoService produtoService;

    @Mock
    private FluxoPedidoAutomaticoService fluxoPedidoAutomaticoService;

    @InjectMocks
    private ProcessamentoRessarcimentoService service;

    @Test
    void iniciar_persisteStatusEmAndamento() {
        ProcessamentoRessarcimento p = ProcessamentoRessarcimento.builder()
                .declarante(Declarante.builder().id(1L).build())
                .anoReferencia("2024")
                .mesReferencia("07")
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO)
                .build();
        p.setId(100L);
        when(processamentoRessarcimentoLifecycle.iniciarEmAndamento(2024, 7)).thenReturn(p);

        ProcessamentoRessarcimento r = service.iniciar(2024, 7);

        assertThat(r.getId()).isEqualTo(100L);
        assertThat(r.getAnoReferencia()).isEqualTo("2024");
        assertThat(r.getMesReferencia()).isEqualTo("07");
        assertThat(r.getStatusExecucao()).isEqualTo(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO);
    }

    @Test
    void executarPipelineCompleto_propagaIdMarcaConcluido() throws IOException, JAXBException {
        ProcessamentoRessarcimento[] holder = new ProcessamentoRessarcimento[1];
        holder[0] = ProcessamentoRessarcimento.builder()
                .declarante(Declarante.builder().id(1L).build())
                .anoReferencia("2024")
                .mesReferencia("07")
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO)
                .build();
        holder[0].setId(42L);
        when(processamentoRessarcimentoLifecycle.iniciarEmAndamento(2024, 7)).thenReturn(holder[0]);
        when(processamentoRepository.findById(42L)).thenAnswer(inv -> Optional.ofNullable(holder[0]));

        ResultadoGeracaoPlanilhaAutomatica planilha = ResultadoGeracaoPlanilhaAutomatica.builder()
                .planilhaXlsx(new byte[] {0x50, 0x4b})
                .build();
        when(produtoPlanilhaAutomaticaService.gerarPlanilhaAutomatica(any(GerarPlanilhaAutomaticaRequest.class), eq(42L)))
                .thenReturn(planilha);
        when(produtoService.importar(any(), eq("planilha_produtos.xlsx"), eq(42L)))
                .thenReturn(ResultadoImportacaoDTO.builder()
                        .totalLinhasComErro(0)
                        .totalLinhasProcessadas(1)
                        .totalPersistidas(1)
                        .build());
        when(fluxoPedidoAutomaticoService.gerarAutomatico(2024, 7, 42L))
                .thenReturn(GerarPedidoAutomaticoResponse.builder()
                        .arquivoPedidoId(9L)
                        .processamentoRessarcimentoId(42L)
                        .status("CONCLUIDO")
                        .build());
        when(produtoService.gerarXmlRetornandoIdArquivo(42L)).thenReturn(77L);

        ProcessamentoRessarcimentoService.ResultadoPipelineProcessamento r =
                service.executarPipelineCompleto(2024, 7, new GerarPlanilhaAutomaticaRequest());

        assertThat(r.getProcessamentoRessarcimentoId()).isEqualTo(42L);
        assertThat(r.getRespostaPedidos().getArquivoPedidoId()).isEqualTo(9L);
        assertThat(r.getArquivoProdutosId()).isEqualTo(77L);
        verify(fluxoPedidoAutomaticoService).gerarAutomatico(2024, 7, 42L);
        verify(produtoService).gerarXmlRetornandoIdArquivo(42L);
        assertThat(holder[0].getStatusExecucao()).isEqualTo(ProcessamentoRessarcimento.STATUS_CONCLUIDO);
        assertThat(holder[0].getDataHoraFim()).isNotNull();
    }

    @Test
    void executarPipelineCompleto_emFalhaMarcaErro() throws IOException {
        ProcessamentoRessarcimento[] holder = new ProcessamentoRessarcimento[1];
        holder[0] = ProcessamentoRessarcimento.builder()
                .declarante(Declarante.builder().id(1L).build())
                .anoReferencia("2024")
                .mesReferencia("01")
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO)
                .build();
        holder[0].setId(7L);
        when(processamentoRessarcimentoLifecycle.iniciarEmAndamento(2024, 1)).thenReturn(holder[0]);
        when(processamentoRepository.findById(7L)).thenAnswer(inv -> Optional.ofNullable(holder[0]));

        when(produtoPlanilhaAutomaticaService.gerarPlanilhaAutomatica(any(GerarPlanilhaAutomaticaRequest.class), eq(7L)))
                .thenThrow(new IOException("falha planilha"));

        assertThatThrownBy(() -> service.executarPipelineCompleto(2024, 1, new GerarPlanilhaAutomaticaRequest()))
                .isInstanceOf(IOException.class);

        assertThat(holder[0].getStatusExecucao()).isEqualTo(ProcessamentoRessarcimento.STATUS_ERRO);
        assertThat(holder[0].getMensagemErro()).contains("falha planilha");
    }

    @Test
    void marcarConcluido_atualizaRegistro() {
        ProcessamentoRessarcimento p = ProcessamentoRessarcimento.builder()
                .declarante(Declarante.builder().id(1L).build())
                .anoReferencia("2024")
                .mesReferencia("01")
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO)
                .build();
        p.setId(5L);
        when(processamentoRepository.findById(5L)).thenReturn(Optional.of(p));

        service.marcarConcluido(5L);

        assertThat(p.getStatusExecucao()).isEqualTo(ProcessamentoRessarcimento.STATUS_CONCLUIDO);
        assertThat(p.getDataHoraFim()).isNotNull();
        verify(processamentoRepository, atLeastOnce()).save(p);
    }
}
