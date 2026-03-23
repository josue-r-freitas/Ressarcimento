package br.com.empresa.ressarcimento.produtos.automatizado;

import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class LeitorResumoNf {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.ROOT);

    public List<ResumoNfLinhaDTO> lerExcel(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return List.of();
            }
            Map<String, Integer> colunas = mapearCabecalhos(headerRow);
            Integer idxChave = colunas.get("CHAVE");
            Integer idxSeq = colunas.get("SEQ. ITEM");
            Integer idxDataApres = colunas.get("DATA APRES.");
            Integer idxCnpj = colunas.get("CNPJ FORNECEDOR");
            Integer idxCodg = colunas.get("CODG. ITEM");
            if (idxChave == null || idxSeq == null || idxDataApres == null || idxCnpj == null || idxCodg == null) {
                throw new IllegalArgumentException(
                        "Planilha resumo deve conter colunas: CHAVE, SEQ. ITEM, DATA APRES., CNPJ FORNECEDOR, CODG. ITEM");
            }
            List<ResumoNfLinhaDTO> linhas = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                int rowNum = r + 1;
                String dataApresStr = getCellString(row.getCell(idxDataApres));
                if (dataApresStr == null || dataApresStr.isBlank()) {
                    continue;
                }
                Optional<LocalDate> dataApres = parseData(dataApresStr, row.getCell(idxDataApres));
                String chave = normalizarChave(getCellString(row.getCell(idxChave)));
                int seqItem = parseInt(getCellString(row.getCell(idxSeq)), row.getCell(idxSeq));
                String codg = getCellString(row.getCell(idxCodg));
                String cnpj = normalizarCnpj(getCellString(row.getCell(idxCnpj)));
                linhas.add(ResumoNfLinhaDTO.builder()
                        .numeroLinhaPlanilha(rowNum)
                        .chave(chave != null ? chave : "")
                        .seqItem(seqItem)
                        .codgItem(codg != null ? codg.trim() : "")
                        .cnpjFornecedor(cnpj != null ? cnpj : "")
                        .dataApresentacao(dataApres.orElse(null))
                        .build());
            }
            linhas.sort(Comparator.comparing(ResumoNfLinhaDTO::getChave).thenComparingInt(ResumoNfLinhaDTO::getSeqItem));
            return linhas;
        }
    }

    private static Map<String, Integer> mapearCabecalhos(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            String raw = cell != null ? DATA_FORMATTER.formatCellValue(cell) : "";
            String norm = normalizarTituloColuna(raw);
            if (!norm.isEmpty()) {
                map.putIfAbsent(norm, c);
            }
        }
        return map;
    }

    static String normalizarTituloColuna(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\u00A0', ' ').trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        return t;
    }

    private static String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return DATA_FORMATTER.formatCellValue(cell);
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private static Optional<LocalDate> parseData(String texto, Cell cell) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                return Optional.of(DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate());
            } catch (Exception ignored) {
            }
        }
        String t = texto.trim();
        List<DateTimeFormatter> formatadores = List.of(
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE);
        for (DateTimeFormatter f : formatadores) {
            try {
                return Optional.of(LocalDate.parse(t, f));
            } catch (DateTimeParseException ignored) {
            }
        }
        return Optional.empty();
    }

    private static int parseInt(String s, Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        if (s == null || s.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String normalizarChave(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\D", "");
    }

    private static String normalizarCnpj(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\D", "");
    }

    /**
     * Se houver mais de um ano/mês distinto em {@code dataApresentacao} não nula, retorna os períodos encontrados.
     */
    public static Optional<String> detectarPeriodosMisturados(List<ResumoNfLinhaDTO> linhas) {
        List<YearMonth> meses = linhas.stream()
                .map(ResumoNfLinhaDTO::getDataApresentacao)
                .filter(d -> d != null)
                .map(YearMonth::from)
                .distinct()
                .sorted()
                .toList();
        if (meses.size() <= 1) {
            return Optional.empty();
        }
        return Optional.of("Períodos distintos em DATA APRES.: " + meses);
    }
}
