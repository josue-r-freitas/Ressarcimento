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
import org.springframework.stereotype.Component;

@Component
public class GeradorXmlPedidos {

    private static final String VERSAO = "2.00";

    public String gerar(Declarante declarante, String anoReferencia, String mesReferencia,
                        List<NotaSaida> notasSaida) throws JAXBException {

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
            Set<String> chavesEntrada = new LinkedHashSet<>();
            for (ItemNotaSaida item : nota.getItens()) {
                itens.add(EnviOperacaoRessarcimento.ItemXml.builder()
                        .codInternoProduto(item.getCodInternoProduto())
                        .numItemNFe(String.valueOf(item.getNumItemNFe()))
                        .chaveNFeEntrada(item.getNotaEntrada() != null ? item.getNotaEntrada().getChaveNFeEntrada() : null)
                        .build());
                if (item.getNotaEntrada() != null) {
                    chavesEntrada.add(item.getNotaEntrada().getChaveNFeEntrada());
                }
            }
            List<NotaEntrada> entradasDestaNota = deduplicarEntradasPorChave(nota);
            List<EnviOperacaoRessarcimento.NFeEntradaXml> nfeEntradas = entradasDestaNota.stream()
                    .map(ne -> EnviOperacaoRessarcimento.NFeEntradaXml.builder()
                            .chaveNFeEntrada(ne.getChaveNFeEntrada())
                            .chaveCTeEntrada(blankToNull(ne.getChaveCTeEntrada()))
                            .chaveMDFeEntrada(blankToNull(ne.getChaveMDFeEntrada()))
                            .build())
                    .collect(Collectors.toList());

            operacoes.add(EnviOperacaoRessarcimento.OperacaoXml.builder()
                    .chaveNFe(nota.getChaveNFe())
                    .listaItens(EnviOperacaoRessarcimento.ListaItens.builder().itens(itens).build())
                    .listaNFeEntrada(EnviOperacaoRessarcimento.ListaNFeEntrada.builder().nfeEntradas(nfeEntradas).build())
                    .build());
        }

        EnviOperacaoRessarcimento root = EnviOperacaoRessarcimento.builder()
                .versao(VERSAO)
                .dadosDeclarante(dados)
                .listaOperacoes(EnviOperacaoRessarcimento.ListaOperacoes.builder().operacoes(operacoes).build())
                .build();

        JAXBContext context = JAXBContext.newInstance(EnviOperacaoRessarcimento.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter writer = new StringWriter();
        marshaller.marshal(root, writer);
        return writer.toString();
    }

    private List<NotaEntrada> deduplicarEntradasPorChave(NotaSaida nota) {
        Set<String> vistas = new LinkedHashSet<>();
        List<NotaEntrada> resultado = new ArrayList<>();
        for (ItemNotaSaida item : nota.getItens()) {
            if (item.getNotaEntrada() != null) {
                String chave = item.getNotaEntrada().getChaveNFeEntrada();
                if (chave != null && !chave.isBlank() && vistas.add(chave)) {
                    resultado.add(item.getNotaEntrada());
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
