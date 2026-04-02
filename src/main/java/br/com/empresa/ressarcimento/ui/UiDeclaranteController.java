package br.com.empresa.ressarcimento.ui;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.api.DeclaranteDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui/declarante")
@RequiredArgsConstructor
public class UiDeclaranteController {

    private final DeclaranteService declaranteService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute(
                "declarante",
                declaranteService.buscarSeExistir().orElseGet(DeclaranteDTO::new));
        model.addAttribute("pageTitle", "Declarante");
        return "ui/declarante/form";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("declarante") DeclaranteDTO declarante,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Declarante");
            return "ui/declarante/form";
        }
        declaranteService.salvar(declarante);
        redirectAttributes.addFlashAttribute("successMessage", "Declarante salvo com sucesso.");
        return "redirect:/ui/declarante";
    }
}
