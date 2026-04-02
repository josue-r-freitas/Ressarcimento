package br.com.empresa.ressarcimento.xml.pedido;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.pedidos.domain.ItemNotaSaida;
import br.com.empresa.ressarcimento.pedidos.domain.NotaEntrada;
import br.com.empresa.ressarcimento.pedidos.domain.NotaSaida;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Monta o XML {@code enviOperacaoRessarcimento} versão 2.00. Regra obrigatória do manual: chaves de entrada só em
 * {@code listaNFeEntrada}, nunca em {@code listaItens/item}.
 */
@Component
@RequiredArgsConstructor
public class GeradorXmlPedidos {

    private static final String VERSAO = "2.00";

    private final ValidadorXmlOperacaoRessarcimento validadorXml;

    public String gerar(Declarante declarante, String anoReferencia, String mesReferencia,
                        List<NotaSaida> notasSaida) throws JAXBException {

        if (notasSaida == null || notasSaida.isEmpty()) {
            throw new IllegalArgumentException(
                    "É necessário ao menos uma NF-e de saída com itens no período para gerar o XML de pedidos.");
        }

        EnviOperacaoRessarcimento.DadosDeclarantePedido dados = EnviOperacaoRessarcimento.DadosDeclarantePedido.builder()
                .cnpjRaiz(declarante.getCnpjRaiz())
                .ieContribuinteDeclarante(declarante.getIeContribuinteDeclarante())
                .razaoSocial(declarante.getRazaoSocial())
                .nomeResponsavel(declarante.getNomeResponsavel())
                .foneResponsavel(declarante.getFoneResponsavel())
                .emailResponsavel(declarante.getEmailResponsavel())
                .anoPeriodoReferencia(anoReferencia)
                .mesPeriodoReferencia(mesReferencia)
                .build();

        List<EnviOperacaoRessarcimento.OperacaoXml> operacoes = new ArrayList<>();
        for (NotaSaida nota : notasSaida) {
            List<EnviOperacaoRessarcimento.ItemXml> itens = new ArrayList<>();
            for (ItemNotaSaida item : nota.getItens()) {
                itens.add(EnviOperacaoRessarcimento.ItemXml.builder()
                        .codInternoProduto(item.getCodInternoProduto())
                        .numItemNFe(String.valueOf(item.getNumItemNFe()))
                        .build());
            }
            if (itens.isEmpty()) {
                continue;
            }
            operacoes.add(EnviOperacaoRessarcimento.OperacaoXml.builder()
                    .chaveNFe(nota.getChaveNFe())
                    .listaItens(EnviOperacaoRessarcimento.ListaItens.builder().itens(itens).build())
                    .build());
        }

        if (operacoes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nenhuma operação com itens encontrada para o período; não é possível gerar o XML de pedidos.");
        }

        List<NotaEntrada> todasEntradas = deduplicarEntradasGlobal(notasSaida);
        EnviOperacaoRessarcimento.ListaNFeEntrada listaNFeRaiz = null;
        if (!todasEntradas.isEmpty()) {
            listaNFeRaiz = EnviOperacaoRessarcimento.ListaNFeEntrada.builder()
                    .nfeEntradas(todasEntradas.stream()
                            .map(ne -> EnviOperacaoRessarcimento.NFeEntradaXml.builder()
                                    .chaveNFeEntrada(ne.getChaveNFeEntrada())
                                    .chaveCTeEntrada(blankToNull(ne.getChaveCTeEntrada()))
                                    .chaveMDFeEntrada(blankToNull(ne.getChaveMDFeEntrada()))
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        }

        EnviOperacaoRessarcimento root = EnviOperacaoRessarcimento.builder()
                .versao(VERSAO)
                .dadosDeclarante(dados)
                .listaOperacoes(EnviOperacaoRessarcimento.ListaOperacoes.builder().operacoes(operacoes).build())
                .listaNFeEntrada(listaNFeRaiz)
                .build();

        JAXBContext context = JAXBContext.newInstance(EnviOperacaoRessarcimento.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        marshaller.marshal(root, writer);
        String xml = writer.toString();
        validadorXml.validar(xml);
        return xml;
    }

    private static List<NotaEntrada> deduplicarEntradasGlobal(List<NotaSaida> notas) {
        Set<String> vistas = new LinkedHashSet<>();
        List<NotaEntrada> resultado = new ArrayList<>();
        for (NotaSaida nota : notas) {
            if (nota.getItens() == null) {
                continue;
            }
            for (ItemNotaSaida item : nota.getItens()) {
                if (item.getNotaEntrada() != null) {
                    String chave = item.getNotaEntrada().getChaveNFeEntrada();
                    if (chave != null && !chave.isBlank() && vistas.add(chave)) {
                        resultado.add(item.getNotaEntrada());
                    }
                }
                if (item.getChavesNfeEntradaConsumidas() != null) {
                    for (String ch : item.getChavesNfeEntradaConsumidas()) {
                        if (ch != null && !ch.isBlank() && vistas.add(ch)) {
                            resultado.add(NotaEntrada.builder().chaveNFeEntrada(ch).build());
                        }
                    }
                }
            }
        }
        return resultado;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s;
    }
}
