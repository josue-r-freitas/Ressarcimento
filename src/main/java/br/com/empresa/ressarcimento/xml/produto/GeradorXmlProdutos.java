package br.com.empresa.ressarcimento.xml.produto;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GeradorXmlProdutos {

    private static final String VERSAO = "9.99";

    public String gerar(Declarante declarante, List<ProdutoMatriz> produtos) throws JAXBException {
        EnviProdutoRessarcimento.DadosDeclaranteProduto dados = EnviProdutoRessarcimento.DadosDeclaranteProduto.builder()
                .cnpjRaiz(declarante.getCnpjRaiz())
                .razaoSocial(declarante.getRazaoSocial())
                .nomeResponsavel(declarante.getNomeResponsavel())
                .foneResponsavel(declarante.getFoneResponsavel())
                .emailResponsavel(declarante.getEmailResponsavel())
                .build();

        List<EnviProdutoRessarcimento.ProdutoXml> lista = produtos.stream()
                .map(p -> EnviProdutoRessarcimento.ProdutoXml.builder()
                        .codInternoProduto(p.getCodInternoProduto())
                        .descricaoProduto(p.getDescricaoProduto())
                        .unidadeInternaProduto(p.getUnidadeInternaProduto())
                        .fatorConversao(formatarFatorConversao(p.getFatorConversao()))
                        .cnpjFornecedor(p.getCnpjFornecedor())
                        .codProdFornecedor(p.getCodProdFornecedor())
                        .unidadeProdutoFornecedor(p.getUnidadeProdutoFornecedor())
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
        return writer.toString();
    }

    private static String formatarFatorConversao(BigDecimal valor) {
        if (valor == null) return "0.000000";
        return String.format("%.6f", valor).replace(",", ".");
    }
}
