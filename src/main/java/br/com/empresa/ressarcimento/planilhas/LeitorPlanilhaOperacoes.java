package br.com.empresa.ressarcimento.planilhas;

import br.com.empresa.ressarcimento.planilhas.dto.OperacaoPlanilhaDTO;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class LeitorPlanilhaOperacoes {

    public List<OperacaoPlanilhaDTO> lerExcel(InputStream inputStream) throws IOException {
        List<OperacaoPlanilhaDTO> linhas = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return linhas;
            rowIterator.next(); // header
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int rowNum = row.getRowNum() + 1;
                OperacaoPlanilhaDTO dto = mapRow(row, rowNum);
                if (dto != null && dto.getChaveNfeSaida() != null && !dto.getChaveNfeSaida().isBlank()) {
                    linhas.add(dto);
                }
            }
        }
        return linhas;
    }

    public List<OperacaoPlanilhaDTO> lerCsv(InputStream inputStream) throws IOException {
        List<OperacaoPlanilhaDTO> linhas = new ArrayList<>();
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        if (lines.length < 2) return linhas;
        for (int i = 1; i < lines.length; i++) {
            String[] values = parseCsvLine(lines[i]);
            if (values.length < 6) continue;
            OperacaoPlanilhaDTO dto = mapCsvLine(values, i + 1);
            if (dto != null && dto.getChaveNfeSaida() != null && !dto.getChaveNfeSaida().isBlank()) {
                linhas.add(dto);
            }
        }
        return linhas;
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            else if ((c == ',' || c == ';') && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else current.append(c);
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    private OperacaoPlanilhaDTO mapRow(Row row, int rowNum) {
        String ano = getCellString(row.getCell(0));
        String mes = getCellString(row.getCell(1));
        String chaveSaida = normalizarChave(getCellString(row.getCell(2)));
        String numItem = row.getCell(3) != null && row.getCell(3).getCellType() == CellType.NUMERIC
                ? String.valueOf((int) row.getCell(3).getNumericCellValue())
                : getCellString(row.getCell(3));
        String codProd = getCellString(row.getCell(4));
        String chaveEntrada = normalizarChave(getCellString(row.getCell(5)));
        String chaveCte = row.getLastCellNum() > 6 ? normalizarChave(getCellString(row.getCell(6))) : "";
        String chaveMdfe = row.getLastCellNum() > 7 ? normalizarChave(getCellString(row.getCell(7))) : "";
        if (chaveSaida == null || chaveSaida.length() != 44) return null;
        return OperacaoPlanilhaDTO.builder()
                .numeroLinha(rowNum)
                .anoReferencia(ano != null ? ano.trim() : "")
                .mesReferencia(mes != null ? mes.trim() : "")
                .chaveNfeSaida(chaveSaida)
                .numItemNfe(numItem != null ? numItem.trim() : "")
                .codInternoProduto(codProd != null ? codProd.trim() : "")
                .chaveNfeEntrada(blankToNull(chaveEntrada))
                .chaveCteEntrada(blankToNull(chaveCte))
                .chaveMdfeEntrada(blankToNull(chaveMdfe))
                .build();
    }

    private OperacaoPlanilhaDTO mapCsvLine(String[] values, int rowNum) {
        String chaveSaida = normalizarChave(values.length > 2 ? values[2] : "");
        if (chaveSaida == null || chaveSaida.length() != 44) return null;
        return OperacaoPlanilhaDTO.builder()
                .numeroLinha(rowNum)
                .anoReferencia(values.length > 0 ? values[0].trim() : "")
                .mesReferencia(values.length > 1 ? values[1].trim() : "")
                .chaveNfeSaida(chaveSaida)
                .numItemNfe(values.length > 3 ? values[3].trim() : "")
                .codInternoProduto(values.length > 4 ? values[4].trim() : "")
                .chaveNfeEntrada(blankToNull(normalizarChave(values.length > 5 ? values[5] : "")))
                .chaveCteEntrada(blankToNull(normalizarChave(values.length > 6 ? values[6] : "")))
                .chaveMdfeEntrada(blankToNull(normalizarChave(values.length > 7 ? values[7] : "")))
                .build();
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.getStringCellValue();
    }

    private static String normalizarChave(String valor) {
        if (valor == null) return null;
        String d = valor.replaceAll("\\D", "");
        return d.length() > 44 ? d.substring(0, 44) : d;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s;
    }
}
