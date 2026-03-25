package br.com.empresa.ressarcimento.xml.produto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@XmlRootElement(
        name = "enviProdutoRessarcimento",
        namespace = "http://www.sefaz.am.gov.br/ressarcimento")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnviProdutoRessarcimento {

    @XmlElement(name = "versao", required = true)
    private String versao;

    @XmlElement(name = "dadosDeclarante", required = true)
    private DadosDeclaranteProduto dadosDeclarante;

    @XmlElement(name = "listaProdutos", required = true)
    @Builder.Default
    private ListaProdutos listaProdutos = new ListaProdutos();

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DadosDeclaranteProduto {
        @XmlElement(name = "cnpjRaiz")
        private String cnpjRaiz;
        @XmlElement(name = "razaoSocial")
        private String razaoSocial;
        @XmlElement(name = "nomeResponsavel")
        private String nomeResponsavel;
        @XmlElement(name = "foneResponsavel")
        private String foneResponsavel;
        @XmlElement(name = "emailResponsavel")
        private String emailResponsavel;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListaProdutos {
        @XmlElement(name = "produto")
        @Builder.Default
        private List<ProdutoXml> produtos = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProdutoXml {
        @XmlElement(name = "codInternoProduto")
        private String codInternoProduto;
        @XmlElement(name = "descricaoProduto")
        private String descricaoProduto;
        @XmlElement(name = "unidadeInternaProduto")
        private String unidadeInternaProduto;
        @XmlElement(name = "fatorConversao")
        private String fatorConversao;
        @XmlElement(name = "cnpjFornecedor")
        private String cnpjFornecedor;
        @XmlElement(name = "codProdFornecedor")
        private String codProdFornecedor;
        @XmlElement(name = "unidadeProdutoFornecedor")
        private String unidadeProdutoFornecedor;
    }
}
