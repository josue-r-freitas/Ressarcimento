package br.com.empresa.ressarcimento.xml.pedido;

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

@XmlRootElement(name = "enviOperacaoRessarcimento")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnviOperacaoRessarcimento {

    @XmlElement(name = "versao", required = true)
    private String versao;

    @XmlElement(name = "dadosDeclarante", required = true)
    private DadosDeclarantePedido dadosDeclarante;

    @XmlElement(name = "listaOperacoes", required = true)
    @Builder.Default
    private ListaOperacoes listaOperacoes = new ListaOperacoes();

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DadosDeclarantePedido {
        @XmlElement(name = "cnpjRaiz")
        private String cnpjRaiz;
        @XmlElement(name = "ieContribuinteDeclarante")
        private String ieContribuinteDeclarante;
        @XmlElement(name = "razaoSocial")
        private String razaoSocial;
        @XmlElement(name = "nomeResponsavel")
        private String nomeResponsavel;
        @XmlElement(name = "foneResponsavel")
        private String foneResponsavel;
        @XmlElement(name = "emailResponsavel")
        private String emailResponsavel;
        @XmlElement(name = "anoPeriodoReferencia")
        private String anoPeriodoReferencia;
        @XmlElement(name = "mesPeriodoReferencia")
        private String mesPeriodoReferencia;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListaOperacoes {
        @XmlElement(name = "operacao")
        @Builder.Default
        private List<OperacaoXml> operacoes = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OperacaoXml {
        @XmlElement(name = "chaveNFe")
        private String chaveNFe;
        @XmlElement(name = "listaItens")
        private ListaItens listaItens;
        @XmlElement(name = "listaNFeEntrada")
        private ListaNFeEntrada listaNFeEntrada;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListaItens {
        @XmlElement(name = "item")
        @Builder.Default
        private List<ItemXml> itens = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemXml {
        @XmlElement(name = "codInternoProduto")
        private String codInternoProduto;
        @XmlElement(name = "numItemNFe")
        private String numItemNFe;
        @XmlElement(name = "chaveNFeEntrada")
        private String chaveNFeEntrada; // opcional - será null para omitir tag
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListaNFeEntrada {
        @XmlElement(name = "nfeEntrada")
        @Builder.Default
        private List<NFeEntradaXml> nfeEntradas = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NFeEntradaXml {
        @XmlElement(name = "chaveNFeEntrada")
        private String chaveNFeEntrada;
        @XmlElement(name = "chaveCTeEntrada")
        private String chaveCTeEntrada; // opcional
        @XmlElement(name = "chaveMDFeEntrada")
        private String chaveMDFeEntrada; // opcional
    }
}
