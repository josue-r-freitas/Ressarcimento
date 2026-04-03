package br.com.empresa.ressarcimento.ui;

import br.com.empresa.ressarcimento.manutencao.ManutencaoDadosService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui/manutencao")
@RequiredArgsConstructor
public class UiManutencaoController {

    private static final String FRASE_CONFIRMACAO = "LIMPAR TUDO";

    private final ManutencaoDadosService manutencaoDadosService;

    @GetMapping("/limpar-dados")
    public String limparDadosForm(Model model) {
        model.addAttribute("pageTitle", "Manutenção — limpar dados");
        model.addAttribute("fraseConfirmacao", FRASE_CONFIRMACAO);
        return "ui/manutencao/limpar-dados";
    }

    @PostMapping("/limpar-dados")
    public String limparDadosPost(
            @RequestParam(required = false) String confirmacao,
            @RequestParam(required = false) Boolean aceito,
            RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(aceito)) {
            redirectAttributes.addFlashAttribute(
                    "limparError", "Marque a caixa de confirmação para prosseguir.");
            return "redirect:/ui/manutencao/limpar-dados";
        }
        if (confirmacao == null || !FRASE_CONFIRMACAO.equals(confirmacao.trim())) {
            redirectAttributes.addFlashAttribute(
                    "limparError",
                    "Digite exatamente a frase indicada: " + FRASE_CONFIRMACAO);
            return "redirect:/ui/manutencao/limpar-dados";
        }
        try {
            manutencaoDadosService.limparTudoExcetoDeclarante();
            redirectAttributes.addFlashAttribute("limparSuccess", Boolean.TRUE);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            redirectAttributes.addFlashAttribute("limparError", "Falha ao limpar: " + msg);
        }
        return "redirect:/ui/manutencao/limpar-dados";
    }
}
