package br.com.empresa.ressarcimento.produtos.automatizado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Geração com resumo + EFD, sem XML na pasta nfes: linha de dados é gerada; unidade_fornecedor fica em branco
 * (somente uCom da NF-e preenche essa coluna).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProdutoPlanilhaAutomaticaSemXmlApiTest {

    private static final String CHAVE = "22345678901234567890123456789012345678901234";

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void pastasSemXml(DynamicPropertyRegistry r) throws Exception {
        Path root = Files.createTempDirectory("ressarcimento-auto-semxml");
        Path resumo = root.resolve("resumo");
        Path efds = root.resolve("efds");
        Path nfes = root.resolve("nfes");
        Files.createDirectories(resumo);
        Files.createDirectories(efds);
        Files.createDirectories(nfes);

        String efd =
                """
                |0190|UN|Unidade|
                |0200|ITEM01|Produto sem xml no disco|||UN|
                |C100|0|0|FORN|55|00|1|999|%s|15012026|
                |C170|1|ITEM01|X|10|UN|
                """
                        .formatted(CHAVE);
        Files.writeString(efds.resolve("efd.txt"), efd, java.nio.charset.StandardCharsets.ISO_8859_1);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet();
            var h = sh.createRow(0);
            h.createCell(0).setCellValue("CHAVE");
            h.createCell(1).setCellValue("SEQ. ITEM");
            h.createCell(2).setCellValue("DATA APRES.");
            h.createCell(3).setCellValue("CNPJ FORNECEDOR");
            h.createCell(4).setCellValue("CODG. ITEM");
            var row = sh.createRow(1);
            row.createCell(0).setCellValue(CHAVE);
            row.createCell(1).setCellValue(1);
            row.createCell(2).setCellValue("15/01/2026");
            row.createCell(3).setCellValue("12.345.678/0001-99");
            row.createCell(4).setCellValue("X");
            try (var out = Files.newOutputStream(resumo.resolve("resumo.xlsx"))) {
                wb.write(out);
            }
        }

        r.add("ressarcimento.resumo-notas-dir", () -> resumo.toAbsolutePath().toString());
        r.add("ressarcimento.efds-dir", () -> efds.toAbsolutePath().toString());
        r.add("ressarcimento.nfes-dir", () -> nfes.toAbsolutePath().toString());
    }

    @Test
    void semXml_geraPlanilhaComUnidadeFornecedorEmBranco() throws Exception {
        byte[] xlsx = mockMvc.perform(
                        post("/api/produtos/gerar-planilha-automatica")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("planilha_produtos.xlsx")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (XSSFWorkbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            var sh = wb.getSheetAt(0);
            Assertions.assertTrue(sh.getLastRowNum() >= 1, "esperado cabeçalho + pelo menos uma linha de dados");
            var dataRow = sh.getRow(1);
            Assertions.assertEquals("ITEM01", dataRow.getCell(0).getStringCellValue());
            var cellUnidForn = dataRow.getCell(6);
            String unidForn =
                    cellUnidForn == null ? "" : cellUnidForn.getStringCellValue().trim();
            Assertions.assertEquals("", unidForn, "sem NF-e, unidade_fornecedor deve ficar em branco");
        }
    }

    @Test
    void semXml_registraLogXmlNaoEncontrado() throws Exception {
        mockMvc.perform(post("/api/produtos/gerar-planilha-automatica")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        String json = mockMvc.perform(get("/api/produtos/logs-geracao-planilha").param("size", "50"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(json).contains("XML_NFE_NAO_ENCONTRADO");
        assertThat(json).contains(CHAVE);
    }
}
