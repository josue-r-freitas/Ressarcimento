package br.com.empresa.ressarcimento.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.empresa.ressarcimento.pedidos.api.GerarPedidoAutomaticoResponse;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoService;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoService.ResultadoPipelineProcessamento;
import br.com.empresa.ressarcimento.produtos.api.GerarPlanilhaAutomaticaRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UiProcessarRessarcimentoController.class)
class UiProcessarRessarcimentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessamentoRessarcimentoService processamentoRessarcimentoService;

    @Test
    void getForm() throws Exception {
        mockMvc.perform(get("/ui/ressarcimento/processar"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/ressarcimento/processar"));
    }

    @Test
    void postValidacaoAnoInvalido() throws Exception {
        mockMvc.perform(post("/ui/ressarcimento/processar")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("ano", "1999")
                        .param("mes", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/ressarcimento/processar"))
                .andExpect(flash().attributeExists("processarError"));
    }

    @Test
    void postSucesso() throws Exception {
        ResultadoPipelineProcessamento r = ResultadoPipelineProcessamento.builder()
                .processamentoRessarcimentoId(1L)
                .arquivoProdutosId(88L)
                .respostaPedidos(GerarPedidoAutomaticoResponse.builder()
                        .idExecucao(2L)
                        .arquivoPedidoId(9L)
                        .status("CONCLUIDO")
                        .avisos(List.of())
                        .build())
                .build();
        when(processamentoRessarcimentoService.executarPipelineCompleto(eq(2024), eq(3), any(GerarPlanilhaAutomaticaRequest.class)))
                .thenReturn(r);

        mockMvc.perform(post("/ui/ressarcimento/processar")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("ano", "2024")
                        .param("mes", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/ressarcimento/processar"))
                .andExpect(flash().attribute("processarSuccess", true))
                .andExpect(flash().attribute("processamentoId", 1L))
                .andExpect(flash().attribute("arquivoPedidoId", 9L))
                .andExpect(flash().attribute("arquivoProdutosId", 88L));

        ArgumentCaptor<GerarPlanilhaAutomaticaRequest> planilhaCap = ArgumentCaptor.forClass(GerarPlanilhaAutomaticaRequest.class);
        verify(processamentoRessarcimentoService).executarPipelineCompleto(eq(2024), eq(3), planilhaCap.capture());
        assertThat(planilhaCap.getValue().getAnoReferencia()).isNull();
        assertThat(planilhaCap.getValue().getMesReferencia()).isNull();
        assertThat(planilhaCap.getValue().getNomeArquivoResumo()).isEqualTo("resumonf.xlsx");
    }
}
