package br.com.empresa.ressarcimento.ui;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.empresa.ressarcimento.manutencao.ManutencaoDadosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UiManutencaoController.class)
class UiManutencaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManutencaoDadosService manutencaoDadosService;

    @Test
    void getForm() throws Exception {
        mockMvc.perform(get("/ui/manutencao/limpar-dados"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/manutencao/limpar-dados"));
    }

    @Test
    void postSemAceiteNaoChamaServico() throws Exception {
        mockMvc.perform(post("/ui/manutencao/limpar-dados")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("confirmacao", "LIMPAR TUDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/manutencao/limpar-dados"))
                .andExpect(flash().attributeExists("limparError"));

        verify(manutencaoDadosService, never()).limparTudoExcetoDeclarante();
    }

    @Test
    void postFraseErradaNaoChamaServico() throws Exception {
        mockMvc.perform(post("/ui/manutencao/limpar-dados")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("aceito", "true")
                        .param("confirmacao", "errado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("limparError"));

        verify(manutencaoDadosService, never()).limparTudoExcetoDeclarante();
    }

    @Test
    void postOkChamaServico() throws Exception {
        mockMvc.perform(post("/ui/manutencao/limpar-dados")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("aceito", "true")
                        .param("confirmacao", "LIMPAR TUDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("limparSuccess", true));

        verify(manutencaoDadosService).limparTudoExcetoDeclarante();
    }
}
