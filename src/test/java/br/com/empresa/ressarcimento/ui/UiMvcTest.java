package br.com.empresa.ressarcimento.ui;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.api.DeclaranteDTO;
import br.com.empresa.ressarcimento.produtos.ProdutoService;
import br.com.empresa.ressarcimento.produtos.automatizado.ProdutoPlanilhaAutomaticaService;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {HomeUiController.class, UiDeclaranteController.class, UiProdutoController.class})
class UiMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeclaranteService declaranteService;

    @MockBean
    private ProdutoService produtoService;

    @MockBean
    private ProdutoPlanilhaAutomaticaService planilhaAutomaticaService;

    @Test
    void homeReturnsOk() throws Exception {
        mockMvc.perform(get("/ui"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/home"))
                .andExpect(model().attribute("pageTitle", "Início"));
    }

    @Test
    void declaranteGetFormWhenEmpty() throws Exception {
        when(declaranteService.buscarSeExistir()).thenReturn(Optional.empty());
        mockMvc.perform(get("/ui/declarante"))
                .andExpect(status().isOk())
                .andExpect(view().name("ui/declarante/form"))
                .andExpect(model().attributeExists("declarante"));
    }

    @Test
    void declaranteGetFormPreFilled() throws Exception {
        DeclaranteDTO dto = DeclaranteDTO.builder()
                .id(1L)
                .cnpjRaiz("12345678")
                .ieContribuinteDeclarante("12345678")
                .razaoSocial("X")
                .nomeResponsavel("Y")
                .foneResponsavel("92999999999")
                .emailResponsavel("a@b.com")
                .build();
        when(declaranteService.buscarSeExistir()).thenReturn(Optional.of(dto));
        mockMvc.perform(get("/ui/declarante"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("declarante", dto));
    }

    @Test
    void produtosImportRedirectsWithFlash() throws Exception {
        when(produtoService.importar(any()))
                .thenReturn(ResultadoImportacaoDTO.builder()
                        .totalLinhasProcessadas(1)
                        .totalLinhasComErro(0)
                        .totalPersistidas(1)
                        .erros(List.of())
                        .build());
        MockMultipartFile file =
                new MockMultipartFile("arquivo", "p.xlsx", "application/octet-stream", new byte[] {1, 2});
        mockMvc.perform(multipart("/ui/produtos/importar").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/produtos"));
    }
}
