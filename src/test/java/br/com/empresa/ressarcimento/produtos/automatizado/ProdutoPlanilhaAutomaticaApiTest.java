package br.com.empresa.ressarcimento.produtos.automatizado;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProdutoPlanilhaAutomaticaApiTest {

    private static final String CHAVE = "12345678901234567890123456789012345678901234";

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void pastasRessarcimento(DynamicPropertyRegistry r) throws Exception {
        Path root = Files.createTempDirectory("ressarcimento-auto");
        Path resumo = root.resolve("resumo");
        Path efds = root.resolve("efds");
        Path nfes = root.resolve("nfes");
        Files.createDirectories(resumo);
        Files.createDirectories(efds);
        Files.createDirectories(nfes);

        String efd =
                """
                |0190|UN|Unidade|
                |0200|ITEM01|Produto integracao|||UN|
                |0220|CX|1,000000|
                |C100|0|0|FORN|55|00|1|999|%s|15012026|
                |C170|1|ITEM01|X|10|UN|
                """
                        .formatted(CHAVE);
        Files.writeString(efds.resolve("efd.txt"), efd, java.nio.charset.StandardCharsets.ISO_8859_1);

        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe>
                    <infNFe Id="NFe%s">
                      <det nItem="1">
                        <prod>
                          <cProd>P001</cProd>
                          <uCom>UN</uCom>
                        </prod>
                      </det>
                    </infNFe>
                  </NFe>
                </nfeProc>
                """
                        .formatted(CHAVE);
        Files.writeString(nfes.resolve(CHAVE + ".xml"), xml);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sh = wb.createSheet();
            var h = sh.createRow(0);
            h.createCell(0).setCellValue("CHAVE");
            h.createCell(1).setCellValue("SEQ. ITEM");
            h.createCell(2).setCellValue("DATA APRES.");
            h.createCell(3).setCellValue("CNPJ FORNECEDOR");
            h.createCell(4).setCellValue("CODG. ITEM");
            h.createCell(5).setCellValue("TRIBUTO");
            var row = sh.createRow(1);
            row.createCell(0).setCellValue(CHAVE);
            row.createCell(1).setCellValue(1);
            row.createCell(2).setCellValue("15/01/2026");
            row.createCell(3).setCellValue("12.345.678/0001-99");
            row.createCell(4).setCellValue("P001");
            row.createCell(5).setCellValue("1380");
            try (var out = Files.newOutputStream(resumo.resolve("resumo.xlsx"))) {
                wb.write(out);
            }
        }

        r.add("ressarcimento.resumo-notas-dir", () -> resumo.toAbsolutePath().toString());
        r.add("ressarcimento.efds-dir", () -> efds.toAbsolutePath().toString());
        r.add("ressarcimento.nfes-dir", () -> nfes.toAbsolutePath().toString());
    }

    @Test
    void post_gerarPlanilhaAutomatica_retornaXlsx() throws Exception {
        mockMvc.perform(
                        post("/api/produtos/gerar-planilha-automatica")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("planilha_produtos.xlsx")));
    }

    @Test
    void get_gerarPlanilhaAutomatica_retornaXlsx() throws Exception {
        mockMvc.perform(get("/api/produtos/gerar-planilha-automatica"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("planilha_produtos.xlsx")));
    }

    @Test
    void get_gerarPlanilhaAutomatica_comNomeNoPath_retornaXlsx() throws Exception {
        mockMvc.perform(get("/api/produtos/gerar-planilha-automatica/planilha_produtos.xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("planilha_produtos.xlsx")));
    }

    @Test
    void get_gerarPlanilhaAutomatica_zip_retornaArquivoZip() throws Exception {
        byte[] body = mockMvc.perform(get("/api/produtos/gerar-planilha-automatica/planilha_produtos.zip"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("planilha_produtos.zip")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/zip")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        Assertions.assertTrue(body.length > 4);
        Assertions.assertEquals('P', (char) body[0]);
        Assertions.assertEquals('K', (char) body[1]);
    }

    @Test
    void get_logsGeracaoPlanilha_retornaPagina() throws Exception {
        mockMvc.perform(get("/api/produtos/logs-geracao-planilha").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
