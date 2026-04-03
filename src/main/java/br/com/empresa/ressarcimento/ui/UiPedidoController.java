package br.com.empresa.ressarcimento.ui;

import br.com.empresa.ressarcimento.pedidos.PedidoService;
import br.com.empresa.ressarcimento.pedidos.api.GerarPedidoAutomaticoResponse;
import br.com.empresa.ressarcimento.pedidos.fluxo.FluxoPedidoAutomaticoService;
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
@RequestMapping("/ui/pedidos")
@RequiredArgsConstructor
public class UiPedidoController {

    private final PedidoService pedidoService;
    private final FluxoPedidoAutomaticoService fluxoPedidoAutomaticoService;

    @GetMapping
    public String importar(Model model) {
        model.addAttribute("pageTitle", "Pedidos — importar");
        return "ui/pedidos/import";
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
            return "redirect:/ui/pedidos";
        }
        ResultadoImportacaoDTO resultado = pedidoService.importar(arquivo);
        redirectAttributes.addFlashAttribute("resultado", resultado);
        return "redirect:/ui/pedidos";
    }

    @GetMapping("/lista")
    public String lista(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) String ano,
            @RequestParam(required = false) String mes,
            Model model) {
        model.addAttribute("page", pedidoService.listar(pageable, ano, mes));
        model.addAttribute("ano", ano);
        model.addAttribute("mes", mes);
        model.addAttribute("pageTitle", "Pedidos — lista");
        return "ui/pedidos/lista";
    }

    @GetMapping("/gerar-xml")
    public String gerarXmlForm(Model model) {
        model.addAttribute("pageTitle", "Pedidos — gerar XML");
        return "ui/pedidos/gerar-xml";
    }

    @PostMapping("/gerar-xml")
    public Object gerarXml(
            @RequestParam String ano, @RequestParam String mes, RedirectAttributes redirectAttributes)
            throws JAXBException {
        try {
            byte[] xml = pedidoService.gerarXml(ano, mes);
            String mesArq = mes != null && mes.length() <= 2 ? mes.trim() : mes;
            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=enviOperacaoRessarcimento_" + ano.trim() + "_" + mesArq + ".xml")
                    .contentType(MediaType.APPLICATION_XML)
                    .body(xml);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/ui/pedidos/gerar-xml";
        }
    }

    @GetMapping("/gerar-automatico")
    public String gerarAutomaticoForm(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            Model model) {
        model.addAttribute("pageTitle", "Pedidos — gerar XML automático (Fluxo B)");
        model.addAttribute("anoPrefill", ano);
        model.addAttribute("mesPrefill", mes);
        return "ui/pedidos/gerar-automatico";
    }

    @PostMapping("/gerar-automatico")
    public String gerarAutomaticoPost(
            @RequestParam int ano, @RequestParam int mes, RedirectAttributes redirectAttributes) {
        try {
            GerarPedidoAutomaticoResponse resp = fluxoPedidoAutomaticoService.gerarAutomatico(ano, mes);
            redirectAttributes.addFlashAttribute("fluxoBSuccess", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("fluxoBIdExecucao", resp.getIdExecucao());
            redirectAttributes.addFlashAttribute("fluxoBArquivoPedidoId", resp.getArquivoPedidoId());
            redirectAttributes.addFlashAttribute("fluxoBStatus", resp.getStatus());
            redirectAttributes.addFlashAttribute("fluxoBAvisos", resp.getAvisos());
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            redirectAttributes.addFlashAttribute("fluxoBError", msg);
        }
        return "redirect:/ui/pedidos/gerar-automatico";
    }

    @GetMapping("/historico")
    public String historico(@PageableDefault(size = 20) Pageable pageable, Model model) {
        model.addAttribute("itens", pedidoService.listarHistorico(pageable));
        model.addAttribute("pageTitle", "Pedidos — histórico");
        return "ui/pedidos/historico";
    }

    @GetMapping("/historico/{id}/download")
    public ResponseEntity<byte[]> downloadHistorico(@PathVariable Long id) {
        byte[] xml = pedidoService.downloadXmlHistorico(id);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=enviOperacaoRessarcimento_" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
