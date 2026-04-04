package br.com.empresa.ressarcimento.produtos.automatizado;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
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

    @Test
    void leExcel_mapeiaCabecalhosOficiaisResumonf_paraStagingEntrada() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet();
            var h = sh.createRow(0);
            int c = 0;
            h.createCell(c++).setCellValue("CHAVE");
            h.createCell(c++).setCellValue("SEQ. ITEM");
            h.createCell(c++).setCellValue("DATA APRES.");
            h.createCell(c++).setCellValue("CNPJ FORNECEDOR");
            h.createCell(c++).setCellValue("CODG. ITEM");
            h.createCell(c++).setCellValue("QTDE. UNITÁRIA");
            h.createCell(c++).setCellValue("VALOR UNITÁRIO");
            h.createCell(c++).setCellValue("CODG. CFOP");
            h.createCell(c++).setCellValue("VALOR IMPOSTO");
            var r1 = sh.createRow(1);
            c = 0;
            r1.createCell(c++).setCellValue("33333333333333333333333333333333333333333333");
            r1.createCell(c++).setCellValue(1);
            r1.createCell(c++).setCellValue("15/01/2026");
            r1.createCell(c++).setCellValue("12345678000199");
            r1.createCell(c++).setCellValue("P1");
            r1.createCell(c++).setCellValue(3.5);
            r1.createCell(c++).setCellValue(12.34);
            r1.createCell(c++).setCellValue("1556");
            r1.createCell(c++).setCellValue(9.87);
            wb.write(bos);
        }
        List<ResumoNfLinhaDTO> linhas =
                leitor.lerExcel(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(linhas).hasSize(1);
        ResumoNfLinhaDTO l = linhas.get(0);
        assertThat(l.getQtdUnitCompra()).isEqualByComparingTo(new BigDecimal("3.5"));
        assertThat(l.getValorUnitario()).isEqualByComparingTo(new BigDecimal("12.34"));
        assertThat(l.getCfop()).isEqualTo("1556");
        assertThat(l.getValorImposto()).isEqualByComparingTo(new BigDecimal("9.87"));
    }

    @Test
    void leExcel_valorImposto_reconheceTextoComMoeda() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet();
            var h = sh.createRow(0);
            int c = 0;
            h.createCell(c++).setCellValue("CHAVE");
            h.createCell(c++).setCellValue("SEQ. ITEM");
            h.createCell(c++).setCellValue("DATA APRES.");
            h.createCell(c++).setCellValue("CNPJ FORNECEDOR");
            h.createCell(c++).setCellValue("CODG. ITEM");
            h.createCell(c++).setCellValue("VALOR IMPOSTO");
            var r1 = sh.createRow(1);
            c = 0;
            r1.createCell(c++).setCellValue("44444444444444444444444444444444444444444444");
            r1.createCell(c++).setCellValue(1);
            r1.createCell(c++).setCellValue("20/01/2026");
            r1.createCell(c++).setCellValue("12345678000199");
            r1.createCell(c++).setCellValue("P1");
            r1.createCell(c++).setCellValue("R$ 20,91");
            wb.write(bos);
        }
        List<ResumoNfLinhaDTO> linhas =
                leitor.lerExcel(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).getValorImposto()).isEqualByComparingTo(new BigDecimal("20.91"));
    }

    @Test
    void leExcel_cabecalhoValorImpostoComAcento() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet();
            var h = sh.createRow(0);
            int c = 0;
            h.createCell(c++).setCellValue("CHAVE");
            h.createCell(c++).setCellValue("SEQ. ITEM");
            h.createCell(c++).setCellValue("DATA APRES.");
            h.createCell(c++).setCellValue("CNPJ FORNECEDOR");
            h.createCell(c++).setCellValue("CODG. ITEM");
            h.createCell(c++).setCellValue("VALOR IMPÓSTO");
            var r1 = sh.createRow(1);
            c = 0;
            r1.createCell(c++).setCellValue("55555555555555555555555555555555555555555555");
            r1.createCell(c++).setCellValue(1);
            r1.createCell(c++).setCellValue("21/01/2026");
            r1.createCell(c++).setCellValue("12345678000199");
            r1.createCell(c++).setCellValue("P1");
            r1.createCell(c++).setCellValue(1.5);
            wb.write(bos);
        }
        List<ResumoNfLinhaDTO> linhas =
                leitor.lerExcel(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).getValorImposto()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    void leExcel_valorImposto_formulaNumerica() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet();
            var h = sh.createRow(0);
            int c = 0;
            h.createCell(c++).setCellValue("CHAVE");
            h.createCell(c++).setCellValue("SEQ. ITEM");
            h.createCell(c++).setCellValue("DATA APRES.");
            h.createCell(c++).setCellValue("CNPJ FORNECEDOR");
            h.createCell(c++).setCellValue("CODG. ITEM");
            h.createCell(c++).setCellValue("VALOR IMPOSTO");
            var r1 = sh.createRow(1);
            c = 0;
            r1.createCell(c++).setCellValue("66666666666666666666666666666666666666666666");
            r1.createCell(c++).setCellValue(1);
            r1.createCell(c++).setCellValue("22/01/2026");
            r1.createCell(c++).setCellValue("12345678000199");
            r1.createCell(c++).setCellValue("P1");
            r1.createCell(c++).setCellFormula("10.5+10.41");
            wb.write(bos);
        }
        List<ResumoNfLinhaDTO> linhas =
                leitor.lerExcel(new ByteArrayInputStream(bos.toByteArray()));
        assertThat(linhas).hasSize(1);
        assertThat(linhas.get(0).getValorImposto()).isEqualByComparingTo(new BigDecimal("20.91"));
    }
}
