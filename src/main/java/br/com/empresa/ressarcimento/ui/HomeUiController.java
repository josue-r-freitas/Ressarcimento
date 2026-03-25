package br.com.empresa.ressarcimento.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui")
public class HomeUiController {

    @GetMapping
    public String home(Model model) {
        model.addAttribute("pageTitle", "Início");
        return "ui/home";
    }
}
