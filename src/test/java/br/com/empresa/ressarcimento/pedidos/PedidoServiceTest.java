package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.pedidos.domain.NotaSaida;
import br.com.empresa.ressarcimento.produtos.ProdutoMatrizRepository;
import br.com.empresa.ressarcimento.shared.exception.DeclaranteNaoEncontradoException;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private NotaSaidaRepository notaSaidaRepository;
    @Mock
    private NotaEntradaRepository notaEntradaRepository;
    @Mock
    private ArquivoPedidoRepository arquivoPedidoRepository;
    @Mock
    private ProdutoMatrizRepository produtoMatrizRepository;
    @Mock
    private br.com.empresa.ressarcimento.planilhas.LeitorPlanilhaOperacoes leitorPlanilha;
    @Mock
    private ValidacaoPlanilhaOperacoes validacaoPlanilha;
    @Mock
    private DeclaranteService declaranteService;
    @Mock
    private br.com.empresa.ressarcimento.xml.pedido.GeradorXmlPedidos geradorXml;

    @InjectMocks
    private PedidoService service;

    @Test
    void gerarXml_lancaQuandoNaoExisteDeclarante() {
        when(declaranteService.getEntidadeOuLanca()).thenThrow(new DeclaranteNaoEncontradoException());
        assertThatThrownBy(() -> service.gerarXml("2024", "01"))
                .isInstanceOf(DeclaranteNaoEncontradoException.class);
    }

    @Test
    void gerarXml_geraXmlEPersisteHistorico() throws JAXBException {
        Declarante decl = Declarante.builder().id(1L).cnpjRaiz("12345678").ieContribuinteDeclarante("12345678")
                .razaoSocial("Teste").nomeResponsavel("A").foneResponsavel("92999999999").emailResponsavel("a@b.com").build();
        when(declaranteService.getEntidadeOuLanca()).thenReturn(decl);
        NotaSaida nota = NotaSaida.builder().chaveNFe("35200108779811000191550010000000011000000018").build();
        when(notaSaidaRepository.findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(1L, "2024", "01"))
                .thenReturn(List.of(nota));
        when(geradorXml.gerar(decl, "2024", "01", List.of(nota)))
                .thenReturn("<?xml version=\"1.0\"?><enviOperacaoRessarcimento/>");
        when(arquivoPedidoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] result = service.gerarXml("2024", "01");

        assertThat(result).isNotEmpty();
        verify(arquivoPedidoRepository).save(any());
    }
}
