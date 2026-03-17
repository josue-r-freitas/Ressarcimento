package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.pedidos.api.ArquivoPedidoDTO;
import br.com.empresa.ressarcimento.pedidos.api.NotaSaidaDTO;
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
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

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
    public ResponseEntity<Page<NotaSaidaDTO>> listar(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) String ano,
            @RequestParam(required = false) String mes) {
        return ResponseEntity.ok(service.listar(pageable, ano, mes));
    }

    @PostMapping("/gerar-xml")
    public ResponseEntity<byte[]> gerarXml(
            @RequestParam String ano,
            @RequestParam String mes) throws JAXBException {
        byte[] xml = service.gerarXml(ano, mes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enviOperacaoRessarcimento_" + ano + "_" + mes + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping("/historico")
    public ResponseEntity<List<ArquivoPedidoDTO>> historico(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.listarHistorico(pageable));
    }

    @GetMapping("/historico/{id}/download")
    public ResponseEntity<byte[]> downloadHistorico(@PathVariable Long id) {
        byte[] xml = service.downloadXmlHistorico(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enviOperacaoRessarcimento_" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
