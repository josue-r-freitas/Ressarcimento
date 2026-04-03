package br.com.empresa.ressarcimento.produtos.automatizado;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class LeitorNfeUcom {

    /**
     * Localiza XML da NF-e pela chave: tenta nomes comuns e depois varre arquivos .xml cuja raiz contenha a chave.
     */
    public Optional<Path> localizarArquivoXml(Path pastaNfes, String chave44) throws IOException {
        if (!Files.isDirectory(pastaNfes) || chave44 == null || chave44.length() != 44) {
            return Optional.empty();
        }
        String[] candidatosNomes = {
            chave44 + ".xml",
            chave44 + "-nfe.xml",
            "NFe" + chave44 + ".xml",
            "NFe" + chave44 + "-procNFe.xml"
        };
        for (String nome : candidatosNomes) {
            Path p = pastaNfes.resolve(nome);
            if (Files.isRegularFile(p)) {
                return Optional.of(p);
            }
        }
        try (Stream<Path> stream = Files.list(pastaNfes)) {
            for (Path p : stream.filter(x -> x.getFileName().toString().toLowerCase().endsWith(".xml")).toList()) {
                String fn = p.getFileName().toString();
                if (fn.contains(chave44)) {
                    return Optional.of(p);
                }
            }
        }
        try (Stream<Path> stream = Files.walk(pastaNfes)) {
            for (Path p : stream.filter(x -> x.getFileName().toString().toLowerCase().endsWith(".xml")).toList()) {
                if (arquivoContemChave(p, chave44)) {
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean arquivoContemChave(Path p, String chave44) {
        try {
            String head = Files.readString(p, java.nio.charset.StandardCharsets.ISO_8859_1);
            return head.contains(chave44);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Extrai uCom do item com nItem = seqItem; se codProdFornecedor não for vazio, reforça correspondência com cProd.
     * A validação de tamanho (1–6 caracteres, manual NF-e) é feita em {@code ProdutoPlanilhaAutomaticaService}.
     */
    public Optional<String> extrairUcom(Path xmlFile, int seqItem, String codProdFornecedor) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = f.newDocumentBuilder();
        try (InputStream in = Files.newInputStream(xmlFile)) {
            Document doc = builder.parse(in);
            NodeList todos = doc.getElementsByTagNameNS("*", "det");
            for (int i = 0; i < todos.getLength(); i++) {
                Node n = todos.item(i);
                if (!(n instanceof Element det)) {
                    continue;
                }
                String nItemAttr = det.getAttribute("nItem");
                int ni = parseInt(nItemAttr);
                if (ni != seqItem) {
                    continue;
                }
                Element prod = primeiroFilhoLocal(det, "prod");
                if (prod == null) {
                    continue;
                }
                String cProd = textoFilho(prod, "cProd");
                if (codProdFornecedor != null
                        && !codProdFornecedor.isBlank()
                        && cProd != null
                        && !cProd.trim().equals(codProdFornecedor.trim())) {
                    continue;
                }
                String uCom = textoFilho(prod, "uCom");
                if (uCom != null && !uCom.isBlank()) {
                    return Optional.of(uCom.trim());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lê {@code ide/dhSaiEnt}, {@code ide/dhEmi} e {@code ide/dEmi} do XML da NF-e (saída ou entrada).
     */
    public Optional<NfeIdeCampos> lerIdeCampos(Path xmlFile) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = f.newDocumentBuilder();
        try (InputStream in = Files.newInputStream(xmlFile)) {
            Document doc = builder.parse(in);
            Element ide = primeiroElementoPorLocalName(doc, "ide");
            if (ide == null) {
                return Optional.empty();
            }
            String dhSaiEnt = trimToNull(textoFilho(ide, "dhSaiEnt"));
            String dhEmi = trimToNull(textoFilho(ide, "dhEmi"));
            String dEmi = trimToNull(textoFilho(ide, "dEmi"));
            return Optional.of(new NfeIdeCampos(dhSaiEnt, dhEmi, dEmi));
        }
    }

    /**
     * Emissão da NF-e a partir do XML de entrada: prioriza {@code dhEmi}, senão {@code dEmi} (apenas o preenchido).
     */
    public Optional<NfeIdeCampos> lerEmissaoIdeEntrada(Path xmlFile) throws Exception {
        return lerIdeCampos(xmlFile);
    }

    private static Element primeiroElementoPorLocalName(Document doc, String local) {
        NodeList list = doc.getElementsByTagNameNS("*", local);
        if (list.getLength() == 0) {
            return null;
        }
        Node n = list.item(0);
        return n instanceof Element e ? e : null;
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    /**
     * Itens cujo CFOP (tag {@code prod/CFOP}) está em {@code cfopsAceitos} — regra do Fluxo B (filtro no XML, não na EFD).
     */
    public List<ItemNfeCfop> listarItensComCfops(Path xmlFile, Set<String> cfopsAceitos) throws Exception {
        List<ItemNfeCfop> saida = new ArrayList<>();
        if (cfopsAceitos == null || cfopsAceitos.isEmpty()) {
            return saida;
        }
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = f.newDocumentBuilder();
        try (InputStream in = Files.newInputStream(xmlFile)) {
            Document doc = builder.parse(in);
            NodeList todos = doc.getElementsByTagNameNS("*", "det");
            for (int i = 0; i < todos.getLength(); i++) {
                Node n = todos.item(i);
                if (!(n instanceof Element det)) {
                    continue;
                }
                String nItemAttr = det.getAttribute("nItem");
                int ni = parseInt(nItemAttr);
                Element prod = primeiroFilhoLocal(det, "prod");
                if (prod == null) {
                    continue;
                }
                String cfop = textoFilho(prod, "CFOP");
                if (cfop == null || !cfopsAceitos.contains(cfop.trim())) {
                    continue;
                }
                String cProd = textoFilho(prod, "cProd");
                String uCom = textoFilho(prod, "uCom");
                BigDecimal qCom = parseDecimal(textoFilho(prod, "qCom"));
                if (ni > 0 && cProd != null && !cProd.isBlank()) {
                    saida.add(new ItemNfeCfop(
                            ni,
                            cfop.trim(),
                            cProd.trim(),
                            qCom != null ? qCom : BigDecimal.ZERO,
                            uCom != null ? uCom.trim() : ""));
                }
            }
        }
        return saida;
    }

    private static BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }

    private static Element primeiroFilhoLocal(Element pai, String local) {
        NodeList children = pai.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c instanceof Element el) {
                String name = el.getLocalName();
                if (name == null) {
                    name = el.getNodeName();
                }
                if (local.equals(name) || name.endsWith(":" + local)) {
                    return el;
                }
            }
        }
        return null;
    }

    private static String textoFilho(Element pai, String local) {
        Element filho = primeiroFilhoLocal(pai, local);
        if (filho == null) {
            return null;
        }
        return filho.getTextContent() != null ? filho.getTextContent().trim() : null;
    }
}
