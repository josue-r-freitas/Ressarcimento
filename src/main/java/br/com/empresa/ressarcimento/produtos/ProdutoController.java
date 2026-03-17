package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.produtos.api.ArquivoProdutosDTO;
import br.com.empresa.ressarcimento.produtos.api.ProdutoDTO;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService service;

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
}
