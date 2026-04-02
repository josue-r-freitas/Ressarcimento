package br.com.empresa.ressarcimento.produtos.automatizado;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class LeitorResumoNfTest {

    private final LeitorResumoNf leitor = new LeitorResumoNf();

    @Test
    void leExcel_filtraDataApresOrdenaPorChaveESeq() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet();
            var h = sh.createRow(0);
            h.createCell(0).setCellValue("CHAVE");
            h.createCell(1).setCellValue("SEQ. ITEM");
            h.createCell(2).setCellValue("DATA APRES.");
            h.createCell(3).setCellValue("CNPJ FORNECEDOR");
            h.createCell(4).setCellValue("CODG. ITEM");
            var r1 = sh.createRow(1);
            r1.createCell(0).setCellValue("22222222222222222222222222222222222222222222");
            r1.createCell(1).setCellValue(2);
            r1.createCell(2).setCellValue("10/01/2026");
            r1.createCell(3).setCellValue("12345678000199");
            r1.createCell(4).setCellValue("X");
            var r2 = sh.createRow(2);
            r2.createCell(0).setCellValue("11111111111111111111111111111111111111111111");
            r2.createCell(1).setCellValue(1);
            r2.createCell(2).setCellValue("10/01/2026");
            r2.createCell(3).setCellValue("12345678000199");
            r2.createCell(4).setCellValue("Y");
            var skip = sh.createRow(3);
            skip.createCell(0).setCellValue("99999999999999999999999999999999999999999999");
            skip.createCell(2).setCellValue("");
            var skipSemCodg = sh.createRow(4);
            skipSemCodg.createCell(0).setCellValue("88888888888888888888888888888888888888888888");
            skipSemCodg.createCell(1).setCellValue(3);
            skipSemCodg.createCell(2).setCellValue("10/01/2026");
            skipSemCodg.createCell(3).setCellValue("12345678000199");
            skipSemCodg.createCell(4).setCellValue("");
            wb.write(bos);
        }
        List<ResumoNfLinhaDTO> linhas =
                leitor.lerExcel(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).getChave()).startsWith("1111");
        assertThat(linhas.get(1).getChave()).startsWith("2222");
    }
}
