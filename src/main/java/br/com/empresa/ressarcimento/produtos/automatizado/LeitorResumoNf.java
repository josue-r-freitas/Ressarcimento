package br.com.empresa.ressarcimento.produtos.automatizado;

import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * Colunas opcionais (além das obrigatórias) para staging Fluxo B — ver {@link ResumoNfLinhaDTO}.
     * Cabeçalhos após {@link #normalizarTituloColuna(String)}.
     */
    private static final String[] COL_NR_NOTA = {"NR. NOTA", "NR NOTA", "NUMERO NOTA", "NÚMERO NOTA"};

    private static final String[] COL_QTD_UNIT = {
        "QTD. UNIT.", "QUANTIDADE", "QTDE. UNIT.", "QTD UNIT", "QUANTIDADE UNITÁRIA", "QUANTIDADE UNITARIA"
    };
    private static final String[] COL_VL_UNIT = {"VL. UNIT.", "VALOR UNIT.", "VALOR UNITÁRIO", "VALOR UNITARIO", "VLR. UNIT."};
    private static final String[] COL_CFOP = {"CFOP"};
    private static final String[] COL_VL_IMPOSTO = {"VL. IMPOSTO.", "VALOR IMPOSTO", "VL. ICMS", "VALOR ICMS"};

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
            Integer idxTributo = colunas.get("TRIBUTO");
            Integer idxNrNota = primeiroIndiceColuna(colunas, COL_NR_NOTA);
            Integer idxQtd = primeiroIndiceColuna(colunas, COL_QTD_UNIT);
            Integer idxVlUnit = primeiroIndiceColuna(colunas, COL_VL_UNIT);
            Integer idxCfop = primeiroIndiceColuna(colunas, COL_CFOP);
            Integer idxVlImposto = primeiroIndiceColuna(colunas, COL_VL_IMPOSTO);
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
                if (codg == null || codg.trim().isEmpty()) {
                    continue;
                }
                String cnpj = normalizarCnpj(getCellString(row.getCell(idxCnpj)));
                String trib = idxTributo != null ? getCellString(row.getCell(idxTributo)) : null;
                String nrNota = idxNrNota != null ? trimOrNull(getCellString(row.getCell(idxNrNota))) : null;
                String rawQtd = idxQtd != null ? getCellString(row.getCell(idxQtd)) : null;
                String rawVl = idxVlUnit != null ? getCellString(row.getCell(idxVlUnit)) : null;
                String rawCfop = idxCfop != null ? getCellString(row.getCell(idxCfop)) : null;
                String rawImp = idxVlImposto != null ? getCellString(row.getCell(idxVlImposto)) : null;
                linhas.add(ResumoNfLinhaDTO.builder()
                        .numeroLinhaPlanilha(rowNum)
                        .chave(chave != null ? chave : "")
                        .seqItem(seqItem)
                        .codgItem(codg.trim())
                        .cnpjFornecedor(cnpj != null ? cnpj : "")
                        .dataApresentacao(dataApres.orElse(null))
                        .tributo(trib != null ? trib.trim() : null)
                        .nrNota(nrNota)
                        .qtdUnitCompra(parseDecimalCell(idxQtd != null ? row.getCell(idxQtd) : null, rawQtd))
                        .valorUnitario(parseDecimalCell(idxVlUnit != null ? row.getCell(idxVlUnit) : null, rawVl))
                        .cfop(trimOrNull(rawCfop))
                        .valorImposto(parseDecimalCell(idxVlImposto != null ? row.getCell(idxVlImposto) : null, rawImp))
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

    private static Integer primeiroIndiceColuna(Map<String, Integer> colunas, String... titulosNormalizados) {
        for (String t : titulosNormalizados) {
            Integer i = colunas.get(t);
            if (i != null) {
                return i;
            }
        }
        return null;
    }

    private static String trimOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static BigDecimal parseDecimalCell(Cell cell, String raw) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim().replace('\u00A0', ' ').replace(" ", "");
        if (t.contains(".") && t.contains(",")) {
            t = t.replace(".", "").replace(',', '.');
        } else if (t.contains(",")) {
            t = t.replace(',', '.');
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
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

}
