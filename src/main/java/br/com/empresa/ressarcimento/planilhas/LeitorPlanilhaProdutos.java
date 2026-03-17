package br.com.empresa.ressarcimento.planilhas;

import br.com.empresa.ressarcimento.planilhas.dto.ProdutoPlanilhaDTO;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
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
public class LeitorPlanilhaProdutos {

    private static final String[] COLUNAS_ESPERADAS = {
            "cod_interno_produto", "descricao_produto", "unidade_interna", "fator_conversao",
            "cnpj_fornecedor", "cod_prod_fornecedor", "unidade_fornecedor"
    };

    public List<ProdutoPlanilhaDTO> lerExcel(InputStream inputStream) throws IOException {
        List<ProdutoPlanilhaDTO> linhas = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) return linhas;
            Row headerRow = rowIterator.next();
            int colCount = headerRow.getLastCellNum();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int rowNum = row.getRowNum() + 1;
                ProdutoPlanilhaDTO dto = mapRow(row, rowNum, colCount);
                if (dto != null) {
                    linhas.add(dto);
                }
            }
        }
        return linhas;
    }

    public List<ProdutoPlanilhaDTO> lerCsv(InputStream inputStream) throws IOException {
        List<ProdutoPlanilhaDTO> linhas = new ArrayList<>();
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = content.split("\\r?\\n");
        if (lines.length < 2) return linhas;
        String[] headers = parseCsvLine(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            String[] values = parseCsvLine(lines[i]);
            if (values.length < 7) continue;
            ProdutoPlanilhaDTO dto = mapCsvLine(values, i + 1);
            if (dto != null) linhas.add(dto);
        }
        return linhas;
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == ',' || c == ';') && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    private ProdutoPlanilhaDTO mapRow(Row row, int rowNum, int colCount) {
        if (colCount < 7) return null;
        String codInterno = getCellString(row.getCell(0));
        String descricao = getCellString(row.getCell(1));
        String unidadeInterna = getCellString(row.getCell(2));
        BigDecimal fator = getCellBigDecimal(row.getCell(3));
        String cnpjFornecedor = normalizarNumerico(getCellString(row.getCell(4)), 14);
        String codProdFornecedor = getCellString(row.getCell(5));
        String unidadeFornecedor = getCellString(row.getCell(6));
        if (codInterno == null || codInterno.isBlank()) return null;
        return ProdutoPlanilhaDTO.builder()
                .numeroLinha(rowNum)
                .codInternoProduto(codInterno.trim())
                .descricaoProduto(descricao != null ? descricao.trim() : "")
                .unidadeInternaProduto(unidadeInterna != null ? unidadeInterna.trim() : "")
                .fatorConversao(fator != null ? fator : BigDecimal.ZERO)
                .cnpjFornecedor(cnpjFornecedor != null ? cnpjFornecedor : "")
                .codProdFornecedor(codProdFornecedor != null ? codProdFornecedor.trim() : "")
                .unidadeProdutoFornecedor(unidadeFornecedor != null ? unidadeFornecedor.trim() : "")
                .build();
    }

    private ProdutoPlanilhaDTO mapCsvLine(String[] values, int rowNum) {
        String codInterno = values.length > 0 ? values[0].trim() : "";
        if (codInterno.isEmpty()) return null;
        BigDecimal fator;
        try {
            fator = new BigDecimal(values.length > 3 ? values[3].replace(",", ".") : "0");
        } catch (NumberFormatException e) {
            fator = BigDecimal.ZERO;
        }
        String cnpj = normalizarNumerico(values.length > 4 ? values[4].trim() : "", 14);
        return ProdutoPlanilhaDTO.builder()
                .numeroLinha(rowNum)
                .codInternoProduto(codInterno)
                .descricaoProduto(values.length > 1 ? values[1].trim() : "")
                .unidadeInternaProduto(values.length > 2 ? values[2].trim() : "")
                .fatorConversao(fator)
                .cnpjFornecedor(cnpj != null ? cnpj : "")
                .codProdFornecedor(values.length > 5 ? values[5].trim() : "")
                .unidadeProdutoFornecedor(values.length > 6 ? values[6].trim() : "")
                .build();
    }

    private static String getCellString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.getStringCellValue();
    }

    private static BigDecimal getCellBigDecimal(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        try {
            return new BigDecimal(cell.getStringCellValue().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalizarNumerico(String valor, int tamanho) {
        if (valor == null) return null;
        String apenasDigitos = valor.replaceAll("\\D", "");
        if (apenasDigitos.length() > tamanho) return apenasDigitos.substring(0, tamanho);
        return apenasDigitos;
    }
}
