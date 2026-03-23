package br.com.empresa.ressarcimento.produtos.automatizado;

import br.com.empresa.ressarcimento.planilhas.dto.ProdutoPlanilhaDTO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class EscritorPlanilhaProdutosExcel {

    private static final String[] CABECALHOS = {
        "cod_interno_produto",
        "descricao_produto",
        "unidade_interna",
        "fator_conversao",
        "cnpj_fornecedor",
        "cod_prod_fornecedor",
        "unidade_fornecedor"
    };

    public byte[] escrever(List<ProdutoPlanilhaDTO> linhas) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Planilha Produtos");
            Row header = sheet.createRow(0);
            for (int i = 0; i < CABECALHOS.length; i++) {
                header.createCell(i).setCellValue(CABECALHOS[i]);
            }
            int r = 1;
            for (ProdutoPlanilhaDTO p : linhas) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(p.getCodInternoProduto());
                row.createCell(1).setCellValue(p.getDescricaoProduto());
                row.createCell(2).setCellValue(p.getUnidadeInternaProduto());
                row.createCell(3).setCellValue(p.getFatorConversao().doubleValue());
                row.createCell(4).setCellValue(p.getCnpjFornecedor());
                row.createCell(5).setCellValue(p.getCodProdFornecedor());
                row.createCell(6).setCellValue(p.getUnidadeProdutoFornecedor());
            }
            for (int i = 0; i < CABECALHOS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        }
    }
}
