package br.com.empresa.ressarcimento.ui;

import br.com.empresa.ressarcimento.produtos.ProdutoService;
import br.com.empresa.ressarcimento.produtos.api.GerarPlanilhaAutomaticaRequest;
import br.com.empresa.ressarcimento.produtos.api.PlanilhaAutomaticaMetricasHeaders;
import br.com.empresa.ressarcimento.produtos.api.ResultadoGeracaoPlanilhaAutomatica;
import br.com.empresa.ressarcimento.produtos.automatizado.ProdutoPlanilhaAutomaticaService;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui/produtos")
@RequiredArgsConstructor
public class UiProdutoController {

    private static final String CONTENT_DISPOSITION_PLANILHA_XLSX =
            "attachment; filename=\"planilha_produtos.xlsx\"";

    private final ProdutoService produtoService;
    private final ProdutoPlanilhaAutomaticaService planilhaAutomaticaService;

    @GetMapping
    public String importar(Model model) {
        model.addAttribute("pageTitle", "Produtos — importar");
        return "ui/produtos/import";
    }

    @PostMapping("/importar")
    public String importarPost(
            @RequestParam("arquivo") MultipartFile arquivo, RedirectAttributes redirectAttributes)
            throws IOException {
        if (arquivo.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "resultado",
                    ResultadoImportacaoDTO.builder()
                            .totalLinhasProcessadas(0)
                            .totalLinhasComErro(0)
                            .totalPersistidas(0)
                            .erros(List.of())
                            .build());
            redirectAttributes.addFlashAttribute("warningMessage", "Nenhum arquivo foi enviado.");
            return "redirect:/ui/produtos";
        }
        ResultadoImportacaoDTO resultado = produtoService.importar(arquivo);
        redirectAttributes.addFlashAttribute("resultado", resultado);
        return "redirect:/ui/produtos";
    }

    @GetMapping("/lista")
    public String lista(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String descricao,
            Model model) {
        model.addAttribute("page", produtoService.listar(pageable, codigo, descricao));
        model.addAttribute("codigo", codigo);
        model.addAttribute("descricao", descricao);
        model.addAttribute("pageTitle", "Produtos — lista");
        return "ui/produtos/lista";
    }

    @GetMapping("/gerar-xml")
    public String gerarXmlForm(Model model) {
        model.addAttribute("pageTitle", "Produtos — gerar XML");
        return "ui/produtos/gerar-xml";
    }

    @PostMapping("/gerar-xml")
    public ResponseEntity<byte[]> gerarXml() throws JAXBException {
        byte[] xml = produtoService.gerarXml();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enviProdutoRessarcimento.xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping("/historico")
    public String historico(@PageableDefault(size = 20) Pageable pageable, Model model) {
        model.addAttribute("itens", produtoService.listarHistorico(pageable));
        model.addAttribute("pageTitle", "Produtos — histórico");
        return "ui/produtos/historico";
    }

    @GetMapping("/historico/{id}/download")
    public ResponseEntity<byte[]> downloadHistorico(@PathVariable Long id) {
        byte[] xml = produtoService.downloadXmlHistorico(id);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=enviProdutoRessarcimento_" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping("/planilha-automatica")
    public String planilhaAutomaticaForm(Model model) {
        model.addAttribute("pageTitle", "Produtos — planilha automática");
        return "ui/produtos/planilha-auto";
    }

    @PostMapping("/gerar-planilha-automatica")
    public ResponseEntity<byte[]> gerarPlanilhaAutomatica(
            @RequestParam(required = false) Integer anoReferencia,
            @RequestParam(required = false) Integer mesReferencia,
            @RequestParam(required = false) String nomeArquivoResumo)
            throws IOException {
        GerarPlanilhaAutomaticaRequest body = GerarPlanilhaAutomaticaRequest.builder()
                .anoReferencia(anoReferencia)
                .mesReferencia(mesReferencia)
                .nomeArquivoResumo(nomeArquivoResumo)
                .build();
        ResultadoGeracaoPlanilhaAutomatica resultado = planilhaAutomaticaService.gerarPlanilhaAutomatica(body);
        HttpHeaders metricas = new HttpHeaders();
        PlanilhaAutomaticaMetricasHeaders.addTo(metricas, resultado);
        return ResponseEntity.ok()
                .headers(metricas)
                .header(HttpHeaders.CONTENT_DISPOSITION, CONTENT_DISPOSITION_PLANILHA_XLSX)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resultado.getPlanilhaXlsx());
    }

    @GetMapping("/logs-geracao")
    public String logsGeracao(@PageableDefault(size = 50) Pageable pageable, Model model) {
        model.addAttribute("page", planilhaAutomaticaService.listarLogs(pageable));
        model.addAttribute("pageTitle", "Produtos — logs de geração");
        return "ui/produtos/logs";
    }
}
