package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.produtos.api.ArquivoProdutosDTO;
import br.com.empresa.ressarcimento.produtos.api.GerarPlanilhaAutomaticaRequest;
import br.com.empresa.ressarcimento.produtos.api.LogGeracaoPlanilhaDTO;
import br.com.empresa.ressarcimento.produtos.api.ProdutoDTO;
import br.com.empresa.ressarcimento.produtos.automatizado.ProdutoPlanilhaAutomaticaService;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "MATRI-NAC e geração automática da planilha de produtos")
public class ProdutoController {

    /**
     * Nome do arquivo na resposta HTTP (sempre com extensão .xlsx). Usado em {@code Content-Disposition} e no
     * arquivo interno quando o download é via .zip.
     */
    public static final String NOME_ARQUIVO_DOWNLOAD_PLANILHA_PRODUTOS = "planilha_produtos.xlsx";

    private static final String CONTENT_DISPOSITION_PLANILHA_XLSX =
            "attachment; filename=\"" + NOME_ARQUIVO_DOWNLOAD_PLANILHA_PRODUTOS + "\"";

    private static final String CONTENT_DISPOSITION_PLANILHA_ZIP = "attachment; filename=\"planilha_produtos.zip\"";

    private final ProdutoService service;
    private final ProdutoPlanilhaAutomaticaService planilhaAutomaticaService;

    private static ResponseEntity<byte[]> respostaDownloadPlanilhaProdutos(byte[] xlsx) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_PLANILHA_XLSX)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    private ResponseEntity<byte[]> gerarPlanilhaAutomaticaResponse(
            Integer anoReferencia, Integer mesReferencia, String nomeArquivoResumo) throws IOException {
        GerarPlanilhaAutomaticaRequest body = GerarPlanilhaAutomaticaRequest.builder()
                .anoReferencia(anoReferencia)
                .mesReferencia(mesReferencia)
                .nomeArquivoResumo(nomeArquivoResumo)
                .build();
        return respostaDownloadPlanilhaProdutos(planilhaAutomaticaService.gerarPlanilhaAutomatica(body));
    }

    /**
     * Postman no Windows costuma ocultar “.xlsx” no diálogo; o .zip costuma aparecer com extensão. Dentro do zip
     * há {@code planilha_produtos.xlsx} com o conteúdo gerado.
     */
    private static byte[] zipComPlanilhaProdutos(byte[] xlsx) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(NOME_ARQUIVO_DOWNLOAD_PLANILHA_PRODUTOS));
            zos.write(xlsx);
            zos.closeEntry();
            zos.finish();
            return baos.toByteArray();
        }
    }

    private ResponseEntity<byte[]> gerarPlanilhaAutomaticaResponseZip(
            Integer anoReferencia, Integer mesReferencia, String nomeArquivoResumo) throws IOException {
        GerarPlanilhaAutomaticaRequest body = GerarPlanilhaAutomaticaRequest.builder()
                .anoReferencia(anoReferencia)
                .mesReferencia(mesReferencia)
                .nomeArquivoResumo(nomeArquivoResumo)
                .build();
        byte[] xlsx = planilhaAutomaticaService.gerarPlanilhaAutomatica(body);
        byte[] zip = zipComPlanilhaProdutos(xlsx);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_PLANILHA_ZIP)
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResultadoImportacaoDTO> importar(@RequestParam("arquivo") MultipartFile arquivo)
            throws IOException {
        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body(ResultadoImportacaoDTO.builder()
                    .totalLinhasProcessadas(0)
                    .totalLinhasComErro(0)
                    .totalPersistidas(0)
                    .erros(List.of())
                    .build());
        }
        ResultadoImportacaoDTO resultado = service.importar(arquivo);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoDTO>> listar(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String descricao) {
        return ResponseEntity.ok(service.listar(pageable, codigo, descricao));
    }

    @PostMapping("/gerar-xml")
    public ResponseEntity<byte[]> gerarXml() throws JAXBException {
        byte[] xml = service.gerarXml();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enviProdutoRessarcimento.xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping("/historico")
    public ResponseEntity<List<ArquivoProdutosDTO>> historico(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listarHistorico(pageable));
    }

    @GetMapping("/historico/{id}/download")
    public ResponseEntity<byte[]> downloadHistorico(@PathVariable Long id) {
        byte[] xml = service.downloadXmlHistorico(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enviProdutoRessarcimento_" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @Operation(summary = "Gera Planilha Produtos (GET; query opcional). Download: planilha_produtos.xlsx (Content-Disposition).")
    @GetMapping(
            value = "/gerar-planilha-automatica",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> gerarPlanilhaAutomaticaGet(
            @RequestParam(required = false) Integer anoReferencia,
            @RequestParam(required = false) Integer mesReferencia,
            @RequestParam(required = false) String nomeArquivoResumo)
            throws IOException {
        return gerarPlanilhaAutomaticaResponse(anoReferencia, mesReferencia, nomeArquivoResumo);
    }

    /**
     * Mesmo processamento do GET simples; a URL termina com o nome do arquivo para clientes (ex.: Postman no Windows)
     * que sugerem o download a partir do último segmento da URL em vez de Content-Disposition.
     */
    @Operation(summary = "Gera Planilha Produtos (GET; URL termina em .xlsx). Download: planilha_produtos.xlsx.")
    @GetMapping(
            value = "/gerar-planilha-automatica/planilha_produtos.xlsx",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> gerarPlanilhaAutomaticaGetNomeNaUrl(
            @RequestParam(required = false) Integer anoReferencia,
            @RequestParam(required = false) Integer mesReferencia,
            @RequestParam(required = false) String nomeArquivoResumo)
            throws IOException {
        return gerarPlanilhaAutomaticaResponse(anoReferencia, mesReferencia, nomeArquivoResumo);
    }

    @Operation(
            summary =
                    "Gera Planilha Produtos empacotada em .zip (recomendado no Postman no Windows se o nome vier sem .xlsx)")
    @GetMapping(value = "/gerar-planilha-automatica/planilha_produtos.zip", produces = "application/zip")
    public ResponseEntity<byte[]> gerarPlanilhaAutomaticaGetZip(
            @RequestParam(required = false) Integer anoReferencia,
            @RequestParam(required = false) Integer mesReferencia,
            @RequestParam(required = false) String nomeArquivoResumo)
            throws IOException {
        return gerarPlanilhaAutomaticaResponseZip(anoReferencia, mesReferencia, nomeArquivoResumo);
    }

    @Operation(summary = "Gera Planilha Produtos (POST; body JSON opcional). Download: planilha_produtos.xlsx (Content-Disposition).")
    @PostMapping("/gerar-planilha-automatica")
    public ResponseEntity<byte[]> gerarPlanilhaAutomatica(
            @RequestBody(required = false) GerarPlanilhaAutomaticaRequest body) throws IOException {
        byte[] xlsx = planilhaAutomaticaService.gerarPlanilhaAutomatica(body);
        return respostaDownloadPlanilhaProdutos(xlsx);
    }

    @Operation(summary = "Lista logs de inconsistências da geração automática da Planilha Produtos")
    @GetMapping("/logs-geracao-planilha")
    public ResponseEntity<Page<LogGeracaoPlanilhaDTO>> logsGeracaoPlanilha(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(planilhaAutomaticaService.listarLogs(pageable));
    }
}
