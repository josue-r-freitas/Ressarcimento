package br.com.empresa.ressarcimento.xml.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.pedidos.domain.ItemNotaSaida;
import br.com.empresa.ressarcimento.pedidos.domain.NotaEntrada;
import br.com.empresa.ressarcimento.pedidos.domain.NotaSaida;
import jakarta.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeradorXmlPedidosXsdTest {

    private static final String CHAVE_SAIDA =
            "35200108779811000191550010000000011000000018"; // 44 dígitos (exemplo estrutural)
    private static final String CHAVE_ENTRADA =
            "35200108779811000191550010000000021000000016";

    private final ValidadorXmlOperacaoRessarcimento validador = new ValidadorXmlOperacaoRessarcimento();
    private final GeradorXmlPedidos gerador = new GeradorXmlPedidos(validador);

    @Test
    void gerar_itemNaoContemChaveNFeEntrada_eConfereXsd() throws JAXBException {
        Declarante decl = Declarante.builder()
                .cnpjRaiz("08779811")
                .ieContribuinteDeclarante("123456789")
                .razaoSocial("Empresa Teste Ltda")
                .nomeResponsavel("João Silva")
                .foneResponsavel("92999999999")
                .emailResponsavel("joao@teste.com")
                .build();

        NotaEntrada ne = NotaEntrada.builder()
                .chaveNFeEntrada(CHAVE_ENTRADA)
                .build();

        ItemNotaSaida item = ItemNotaSaida.builder()
                .codInternoProduto("ITEM01")
                .numItemNFe(1)
                .notaEntrada(ne)
                .build();

        NotaSaida nota = NotaSaida.builder()
                .chaveNFe(CHAVE_SAIDA)
                .itens(new ArrayList<>(List.of(item)))
                .build();

        String xml = gerador.gerar(decl, "2024", "01", List.of(nota));

        assertThat(xml).contains("xmlns=\"http://www.sefaz.am.gov.br/ressarcimento\"");
        assertThat(xml).contains("<versao>2.00</versao>");
        assertThat(xml).contains("<listaNFeEntrada>");
        assertThat(xml).contains("<chaveNFeEntrada>" + CHAVE_ENTRADA);
        // Requisito do manual: nada de chave de entrada dentro de listaItens/item (XSD coloca listaNFeEntrada no raiz)
        int idxListaItens = xml.indexOf("<listaItens>");
        int idxFimListaItens = xml.indexOf("</listaItens>");
        assertThat(idxListaItens).isGreaterThan(0);
        assertThat(idxFimListaItens).isGreaterThan(idxListaItens);
        String blocoItens = xml.substring(idxListaItens, idxFimListaItens + "</listaItens>".length());
        assertThat(blocoItens).doesNotContain("chaveNFeEntrada");

        validador.validar(xml);
    }

    @Test
    void gerar_semNotas_lanca() throws JAXBException {
        Declarante decl = Declarante.builder()
                .cnpjRaiz("08779811")
                .ieContribuinteDeclarante("123456789")
                .razaoSocial("Empresa Teste Ltda")
                .nomeResponsavel("João Silva")
                .foneResponsavel("92999999999")
                .emailResponsavel("joao@teste.com")
                .build();
        assertThatThrownBy(() -> gerador.gerar(decl, "2024", "01", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos uma NF-e");
    }
}
