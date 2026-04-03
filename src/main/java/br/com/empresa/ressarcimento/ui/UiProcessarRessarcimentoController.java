package br.com.empresa.ressarcimento.ui;

import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoService;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoService.ResultadoPipelineProcessamento;
import br.com.empresa.ressarcimento.produtos.api.GerarPlanilhaAutomaticaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui/ressarcimento")
@RequiredArgsConstructor
public class UiProcessarRessarcimentoController {

    private final ProcessamentoRessarcimentoService processamentoRessarcimentoService;

    @GetMapping("/processar")
    public String form(Model model) {
        model.addAttribute("pageTitle", "Processar ressarcimento");
        return "ui/ressarcimento/processar";
    }

    @PostMapping("/processar")
    public String processar(
            @RequestParam int ano,
            @RequestParam int mes,
            RedirectAttributes redirectAttributes) {
        if (ano < 2000 || ano > 2100 || mes < 1 || mes > 12) {
            redirectAttributes.addFlashAttribute(
                    "processarError", "Informe um ano entre 2000 e 2100 e um mês entre 1 e 12.");
            return "redirect:/ui/ressarcimento/processar";
        }
        GerarPlanilhaAutomaticaRequest planilhaReq = GerarPlanilhaAutomaticaRequest.builder()
                .anoReferencia(null)
                .mesReferencia(null)
                .nomeArquivoResumo("resumonf.xlsx")
                .build();
        try {
            ResultadoPipelineProcessamento r =
                    processamentoRessarcimentoService.executarPipelineCompleto(ano, mes, planilhaReq);
            redirectAttributes.addFlashAttribute("processarSuccess", Boolean.TRUE);
            redirectAttributes.addFlashAttribute("processamentoId", r.getProcessamentoRessarcimentoId());
            redirectAttributes.addFlashAttribute("arquivoPedidoId", r.getRespostaPedidos().getArquivoPedidoId());
            redirectAttributes.addFlashAttribute("arquivoProdutosId", r.getArquivoProdutosId());
            redirectAttributes.addFlashAttribute("fluxoBStatus", r.getRespostaPedidos().getStatus());
            redirectAttributes.addFlashAttribute("fluxoBAvisos", r.getRespostaPedidos().getAvisos());
            redirectAttributes.addFlashAttribute("processarAno", ano);
            redirectAttributes.addFlashAttribute("processarMes", mes);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            redirectAttributes.addFlashAttribute("processarError", msg);
        }
        return "redirect:/ui/ressarcimento/processar";
    }
}
