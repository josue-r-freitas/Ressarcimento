package br.com.empresa.ressarcimento.xml.produto;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class GeradorXmlProdutos {

    private static final String VERSAO = "1.00";

    private final ValidadorXmlProdutoRessarcimento validadorXml;

    public String gerar(Declarante declarante, List<ProdutoMatriz> produtos) throws JAXBException {
        if (produtos == null || produtos.isEmpty()) {
            throw new IllegalArgumentException(
                    "É necessário ao menos um produto na matriz para gerar o XML (o XSD exige ao menos um <produto>).");
        }

        EnviProdutoRessarcimento.DadosDeclaranteProduto dados = EnviProdutoRessarcimento.DadosDeclaranteProduto.builder()
                .cnpjRaiz(normalizarCnpjRaiz(declarante.getCnpjRaiz()))
                .razaoSocial(declarante.getRazaoSocial())
                .nomeResponsavel(declarante.getNomeResponsavel())
                .foneResponsavel(declarante.getFoneResponsavel())
                .emailResponsavel(declarante.getEmailResponsavel())
                .build();

        List<EnviProdutoRessarcimento.ProdutoXml> lista = produtos.stream()
                .map(p -> EnviProdutoRessarcimento.ProdutoXml.builder()
                        .codInternoProduto(p.getCodInternoProduto())
                        .descricaoProduto(normalizarDescricao(p.getDescricaoProduto()))
                        .unidadeInternaProduto(normalizarUnidade(p.getUnidadeInternaProduto(), "unidadeInternaProduto"))
                        .fatorConversao(formatarFatorConversao(p.getFatorConversao()))
                        .cnpjFornecedor(normalizarCnpjFornecedor(p.getCnpjFornecedor()))
                        .codProdFornecedor(p.getCodProdFornecedor())
                        .unidadeProdutoFornecedor(
                                normalizarUnidade(p.getUnidadeProdutoFornecedor(), "unidadeProdutoFornecedor"))
                        .build())
                .collect(Collectors.toList());

        EnviProdutoRessarcimento.ListaProdutos listaProdutos = EnviProdutoRessarcimento.ListaProdutos.builder()
                .produtos(lista)
                .build();

        EnviProdutoRessarcimento root = EnviProdutoRessarcimento.builder()
                .versao(VERSAO)
                .dadosDeclarante(dados)
                .listaProdutos(listaProdutos)
                .build();

        JAXBContext context = JAXBContext.newInstance(EnviProdutoRessarcimento.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        marshaller.marshal(root, writer);
        String xml = writer.toString();
        validadorXml.validar(xml);
        return xml;
    }

    private static String normalizarCnpjRaiz(String s) {
        if (!StringUtils.hasText(s)) {
            throw new IllegalArgumentException("CNPJ raiz do declarante é obrigatório.");
        }
        String d = s.replaceAll("\\D", "");
        if (d.length() != 8) {
            throw new IllegalArgumentException("CNPJ raiz deve ter 8 dígitos (TCnpjRaiz).");
        }
        return d;
    }

    private static String normalizarCnpjFornecedor(String s) {
        if (!StringUtils.hasText(s)) {
            throw new IllegalArgumentException("CNPJ do fornecedor é obrigatório.");
        }
        String d = s.replaceAll("\\D", "");
        if (d.length() != 14) {
            throw new IllegalArgumentException("CNPJ do fornecedor deve ter 14 dígitos (TCnpj).");
        }
        return d;
    }

    private static String normalizarUnidade(String u, String campo) {
        if (!StringUtils.hasText(u)) {
            throw new IllegalArgumentException(campo + " é obrigatório (TUnidProduto: 1 a 6 caracteres).");
        }
        String t = u.trim();
        if (t.length() > 6) {
            throw new IllegalArgumentException(
                    campo + " deve ter entre 1 e 6 caracteres (TUnidProduto), valor: \"" + u + "\".");
        }
        return t;
    }

    private static String normalizarDescricao(String s) {
        if (!StringUtils.hasText(s)) {
            throw new IllegalArgumentException("Descrição do produto é obrigatória.");
        }
        String t = s.trim();
        if (t.length() > 120) {
            return t.substring(0, 120);
        }
        return t;
    }

    /** Formata fator respeitando {@code TFatorConversao}: string com 1 a 9 caracteres. */
    static String formatarFatorConversao(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        for (int scale = 8; scale >= 0; scale--) {
            BigDecimal r = valor.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros();
            String plain = r.toPlainString();
            if (plain.length() <= 9) {
                return plain;
            }
        }
        String intOnly = valor.setScale(0, RoundingMode.HALF_UP).toPlainString();
        if (intOnly.length() <= 9) {
            return intOnly;
        }
        throw new IllegalArgumentException(
                "Fator de conversão não pode ser representado em 9 caracteres (TFatorConversao): " + valor);
    }
}
