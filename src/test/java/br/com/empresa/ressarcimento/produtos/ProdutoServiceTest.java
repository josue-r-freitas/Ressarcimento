package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.produtos.domain.ArquivoProdutos;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import br.com.empresa.ressarcimento.shared.exception.DeclaranteNaoEncontradoException;
import br.com.empresa.ressarcimento.xml.produto.GeradorXmlProdutos;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoMatrizRepository produtoRepository;
    @Mock
    private ArquivoProdutosRepository arquivoRepository;
    @Mock
    private br.com.empresa.ressarcimento.planilhas.LeitorPlanilhaProdutos leitorPlanilha;
    @Mock
    private DeclaranteService declaranteService;
    @Mock
    private GeradorXmlProdutos geradorXml;
    @Mock
    private MultipartFile arquivo;

    @InjectMocks
    private ProdutoService service;

    @Test
    void gerarXml_lancaQuandoNaoExisteDeclarante() {
        when(declaranteService.getEntidadeOuLanca()).thenThrow(new DeclaranteNaoEncontradoException());
        assertThatThrownBy(() -> service.gerarXml())
                .isInstanceOf(DeclaranteNaoEncontradoException.class);
    }

    @Test
    void gerarXml_geraXmlEPersisteHistorico() throws JAXBException {
        Declarante decl = Declarante.builder().id(1L).cnpjRaiz("12345678").razaoSocial("Teste").nomeResponsavel("A").foneResponsavel("92999999999").emailResponsavel("a@b.com").ieContribuinteDeclarante("12345678").build();
        when(declaranteService.getEntidadeOuLanca()).thenReturn(decl);
        when(produtoRepository.findAllByOrderByCodInternoProduto()).thenReturn(List.of());
        when(geradorXml.gerar(decl, List.of())).thenReturn("<?xml version=\"1.0\"?><enviProdutoRessarcimento/>");
        when(arquivoRepository.save(any(ArquivoProdutos.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] result = service.gerarXml();

        assertThat(result).isNotEmpty();
        verify(arquivoRepository).save(any(ArquivoProdutos.class));
    }

    @Test
    void importar_retornaErrosQuandoLinhaInvalida() throws IOException {
        ProdutoMatriz p = ProdutoMatriz.builder()
                .codInternoProduto("P1")
                .descricaoProduto("Produto 1")
                .unidadeInternaProduto("UN")
                .fatorConversao(BigDecimal.ONE)
                .cnpjFornecedor("12345678901234")
                .codProdFornecedor("F1")
                .unidadeProdutoFornecedor("UN")
                .build();
        var dto = br.com.empresa.ressarcimento.planilhas.dto.ProdutoPlanilhaDTO.builder()
                .numeroLinha(2)
                .codInternoProduto("P1")
                .descricaoProduto("Produto 1")
                .unidadeInternaProduto("UN")
                .fatorConversao(BigDecimal.ONE)
                .cnpjFornecedor("12345678901234")
                .codProdFornecedor("F1")
                .unidadeProdutoFornecedor("UN")
                .build();
        when(arquivo.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(arquivo.getOriginalFilename()).thenReturn("planilha.xlsx");
        when(leitorPlanilha.lerExcel(any())).thenReturn(List.of(dto));
        when(produtoRepository.save(any(ProdutoMatriz.class))).thenReturn(p);

        ResultadoImportacaoDTO resultado = service.importar(arquivo);

        assertThat(resultado.getTotalLinhasProcessadas()).isEqualTo(1);
        assertThat(resultado.getTotalPersistidas()).isEqualTo(1);
        assertThat(resultado.getErros()).isEmpty();
        verify(produtoRepository).save(any(ProdutoMatriz.class));
    }
}
