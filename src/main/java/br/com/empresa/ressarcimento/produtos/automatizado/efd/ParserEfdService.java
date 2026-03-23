package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Lê arquivos EFD ICMS/IPI (SPED) e monta índices C100/C170 e 0200/0220.
 * Campos conforme Guia Prático EFD ICMS/IPI (posições após o tipo de registro, separador |).
 */
@Service
public class ParserEfdService {

    private static final Charset CHARSET_EFD = StandardCharsets.ISO_8859_1;

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
        String currentChave = null;
        NotaEfd notaAtual = null;
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
                        if (notaAtual != null && currentChave != null && currentChave.length() == 44) {
                            indice.mergeNota(currentChave, notaAtual);
                        }
                        currentChave = p.length > 9 ? p[9].trim() : "";
                        notaAtual = new NotaEfd();
                    }
                    case "C170" -> {
                        if (notaAtual == null) {
                            continue;
                        }
                        if (p.length < 7) {
                            continue;
                        }
                        int numItem = parseIntSafe(p[2]);
                        String codItem = p[3].trim();
                        String unid = p[6].trim();
                        if (numItem > 0 && !codItem.isEmpty() && !unid.isEmpty()) {
                            notaAtual.putItem(new C170Linha(numItem, codItem, unid));
                        }
                    }
                    case "0200" -> {
                        if (p.length < 4) {
                            continue;
                        }
                        ultimoCod0200 = p[2].trim();
                        String descr = p[3].trim();
                        indice.putInfoItem(
                                ultimoCod0200,
                                InfoItemSped.builder().descrItem(descr).fatorConversao0220(null).build());
                    }
                    case "0220" -> {
                        if (ultimoCod0200 == null || p.length < 4) {
                            continue;
                        }
                        BigDecimal fat = parseDecimalSafe(p[3]);
                        InfoItemSped existente = indice.infoItem(ultimoCod0200).orElse(null);
                        String descr = existente != null ? existente.getDescrItem() : "";
                        if (existente == null || existente.getFatorConversao0220() == null) {
                            indice.putInfoItem(
                                    ultimoCod0200,
                                    InfoItemSped.builder()
                                            .descrItem(descr)
                                            .fatorConversao0220(fat)
                                            .build());
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        if (notaAtual != null && currentChave != null && currentChave.length() == 44) {
            indice.mergeNota(currentChave, notaAtual);
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
}
