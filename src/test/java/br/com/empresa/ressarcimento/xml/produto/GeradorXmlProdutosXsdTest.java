package br.com.empresa.ressarcimento.xml.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import jakarta.xml.bind.JAXBException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeradorXmlProdutosXsdTest {

    private final ValidadorXmlProdutoRessarcimento validador = new ValidadorXmlProdutoRessarcimento();
    private final GeradorXmlProdutos gerador = new GeradorXmlProdutos(validador);

    @Test
    void gerar_xmlConfereComXsdEversao100() throws JAXBException {
        Declarante decl = Declarante.builder()
                .cnpjRaiz("12345678")
                .razaoSocial("Empresa Teste Ltda")
                .nomeResponsavel("João Silva")
                .foneResponsavel("92999999999")
                .emailResponsavel("joao@teste.com")
                .build();
        ProdutoMatriz p = ProdutoMatriz.builder()
                .codInternoProduto("P001")
                .descricaoProduto("Produto teste para XSD")
                .unidadeInternaProduto("UN")
                .fatorConversao(new BigDecimal("1.000000"))
                .cnpjFornecedor("12345678000199")
                .codProdFornecedor("FORN001")
                .unidadeProdutoFornecedor("UN")
                .build();

        String xml = gerador.gerar(decl, List.of(p));

        assertThat(xml).contains("xmlns=\"http://www.sefaz.am.gov.br/ressarcimento\"");
        assertThat(xml).contains("<versao>1.00</versao>");
        validador.validar(xml);
    }

    @Test
    void formatarFatorConversao_respeita9Caracteres() {
        assertThat(GeradorXmlProdutos.formatarFatorConversao(new BigDecimal("1.5"))).isEqualTo("1.5");
        assertThat(GeradorXmlProdutos.formatarFatorConversao(new BigDecimal("1.000000"))).hasSizeLessThanOrEqualTo(9);
        assertThat(GeradorXmlProdutos.formatarFatorConversao(null)).isEqualTo("0");
    }

    @Test
    void gerar_semProdutos_lanca() {
        Declarante decl = Declarante.builder()
                .cnpjRaiz("12345678")
                .razaoSocial("Empresa Teste Ltda")
                .nomeResponsavel("João Silva")
                .foneResponsavel("92999999999")
                .emailResponsavel("joao@teste.com")
                .build();
        assertThatThrownBy(() -> gerador.gerar(decl, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ao menos um produto");
    }
}
