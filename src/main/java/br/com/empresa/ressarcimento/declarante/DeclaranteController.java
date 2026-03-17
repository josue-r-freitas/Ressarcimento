package br.com.empresa.ressarcimento.declarante;

import br.com.empresa.ressarcimento.declarante.api.DeclaranteDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/declarante")
@RequiredArgsConstructor
public class DeclaranteController {

    private final DeclaranteService service;

    @PostMapping
    public ResponseEntity<DeclaranteDTO> criarOuAtualizar(@Valid @RequestBody DeclaranteDTO dto) {
        DeclaranteDTO salvo = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<DeclaranteDTO> consultar() {
        return ResponseEntity.ok(service.buscar());
    }
}
