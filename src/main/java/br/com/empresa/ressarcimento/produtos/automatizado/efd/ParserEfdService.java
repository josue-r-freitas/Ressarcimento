package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Lê EFD ICMS/IPI: C100 entrada/saída (NF-e 55), C170, 0190, 0200 (UNID_INV), 0220.
 */
@Service
public class ParserEfdService {

    private static final Charset CHARSET_EFD = StandardCharsets.ISO_8859_1;
    private static final DateTimeFormatter DT_DDMMAAAA = DateTimeFormatter.ofPattern("ddMMyyyy");

    public EfdIndice carregarDiretorio(Path diretorio) throws IOException {
        if (!Files.isDirectory(diretorio)) {
            throw new IOException("Diretório EFD inexistente ou inválido: " + diretorio);
        }
        EfdIndice indice = new EfdIndice();
        try (Stream<Path> paths = Files.list(diretorio)) {
            for (Path arquivo : paths.filter(ParserEfdService::isArquivoEfdCandidato).sorted().toList()) {
                processarArquivo(arquivo, indice);
            }
        }
        return indice;
    }

    public static boolean isArquivoEfdCandidato(Path p) {
        if (!Files.isRegularFile(p)) {
            return false;
        }
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".txt") || n.endsWith(".efd") || !n.contains(".");
    }

    void processarArquivo(Path arquivo, EfdIndice indice) throws IOException {
        C100Context ctx = new C100Context();
        String ultimoCod0200 = null;

        try (BufferedReader reader = Files.newBufferedReader(arquivo, CHARSET_EFD)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = line.split("\\|", -1);
                if (p.length < 2) {
                    continue;
                }
                String reg = p[1].trim();
                switch (reg) {
                    case "C100" -> {
                        ctx.flushPara(indice);
                        ctx.chave = p.length > 9 ? p[9].trim() : "";
                        ctx.nota = new NotaEfd();
                        ctx.indOper = p.length > 2 ? p[2].trim() : "";
                        ctx.codMod = p.length > 5 ? p[5].trim() : "";
                        ctx.dtDocRaw = p.length > 10 ? p[10].trim() : "";
                    }
                    case "C170" -> {
                        if (ctx.nota == null) {
                            continue;
                        }
                        if (p.length < 7) {
                            continue;
                        }
                        int numItem = parseIntSafe(p[2]);
                        String codItem = p[3].trim();
                        BigDecimal qtd = parseDecimalSafe(p[5]);
                        String unid = p[6].trim();
                        if (numItem > 0 && !codItem.isEmpty() && !unid.isEmpty()) {
                            BigDecimal vlUnit = p.length > 7 ? parseDecimalSafe(p[7]) : null;
                            String cfop = normalizarCfopC170(p, 11);
                            // Após CFOP (p[11]) e COD_NAT (p[12]): VL_BC_ICMS, ALIQ_ICMS, VL_ICMS (p[13]–p[15]).
                            BigDecimal vlIcms = p.length > 15 ? parseDecimalSafe(p[15]) : null;
                            ctx.nota.putItem(new C170Linha(numItem, codItem, unid, qtd, vlUnit, cfop, vlIcms));
                        }
                    }
                    case "0190" -> {
                        if (p.length >= 4) {
                            indice.putUnidade0190(p[2].trim(), p[3].trim());
                        }
                    }
                    case "0200" -> {
                        if (p.length < 4) {
                            continue;
                        }
                        ultimoCod0200 = p[2].trim();
                        String descr = p[3].trim();
                        String unidInv = p.length > 6 ? p[6].trim() : "";
                        indice.putInfoItem(
                                ultimoCod0200,
                                InfoItemSped.builder()
                                        .descrItem(descr)
                                        .unidInv(unidInv)
                                        .fatorConversao0220(null)
                                        .build());
                    }
                    case "0220" -> {
                        if (ultimoCod0200 == null || p.length < 4) {
                            continue;
                        }
                        BigDecimal fat = parseDecimalSafe(p[3]);
                        InfoItemSped existente = indice.infoItem(ultimoCod0200).orElse(null);
                        String descr = existente != null ? existente.getDescrItem() : "";
                        String unidInv = existente != null ? existente.getUnidInv() : "";
                        if (existente == null || existente.getFatorConversao0220() == null) {
                            indice.putInfoItem(
                                    ultimoCod0200,
                                    InfoItemSped.builder()
                                            .descrItem(descr)
                                            .unidInv(unidInv)
                                            .fatorConversao0220(fat)
                                            .build());
                        }
                    }
                    case "C190" -> {
                        // Totais por CFOP/CST na EFD; Fluxos A/B usam CFOP do XML da NF-e (saída) e C170 nas entradas.
                    }
                    default -> {
                    }
                }
            }
        }
        ctx.flushPara(indice);
    }

    private static final class C100Context {
        String chave;
        NotaEfd nota;
        String indOper = "";
        String codMod = "";
        String dtDocRaw = "";

        void flushPara(EfdIndice indice) {
            if (chave == null || chave.length() != 44 || nota == null) {
                return;
            }
            if ("0".equals(indOper) && "55".equals(codMod)) {
                indice.mergeNotaEntrada(chave, nota);
            } else if ("1".equals(indOper) && "55".equals(codMod)) {
                indice.mergeNotaSaida(chave, nota, parseDataDocumento(dtDocRaw));
            }
        }
    }

    private static LocalDate parseDataDocumento(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), DT_DDMMAAAA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static BigDecimal parseDecimalSafe(String s) {
        try {
            String t = s.trim().replace(",", ".");
            if (t.isEmpty()) {
                return null;
            }
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * CFOP no C170: após CST_ICMS em {@code p[10]} (índice {@code p[11]}).
     */
    private static String normalizarCfopC170(String[] p, int idxCfop) {
        if (p.length <= idxCfop) {
            return null;
        }
        String raw = p[idxCfop].trim();
        if (raw.isEmpty()) {
            return null;
        }
        String d = raw.replaceAll("\\D", "");
        if (d.isEmpty()) {
            return null;
        }
        if (d.length() >= 4) {
            return d.substring(0, 4);
        }
        return String.format("%4s", d).replace(' ', '0');
    }
}
