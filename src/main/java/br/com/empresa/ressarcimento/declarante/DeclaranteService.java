package br.com.empresa.ressarcimento.declarante;

import br.com.empresa.ressarcimento.declarante.api.DeclaranteDTO;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.shared.exception.DeclaranteNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeclaranteService {

    private final DeclaranteRepository repository;

    @Transactional(readOnly = true)
    public DeclaranteDTO buscar() {
        Declarante entidade = repository.findAll().stream().findFirst()
                .orElseThrow(DeclaranteNaoEncontradoException::new);
        return toDTO(entidade);
    }

    @Transactional(readOnly = true)
    public Declarante getEntidadeOuLanca() {
        return repository.findAll().stream().findFirst()
                .orElseThrow(DeclaranteNaoEncontradoException::new);
    }

    @Transactional
    public DeclaranteDTO salvar(DeclaranteDTO dto) {
        Declarante entidade = repository.findByCnpjRaiz(dto.getCnpjRaiz())
                .map(existing -> {
                    existing.setIeContribuinteDeclarante(dto.getIeContribuinteDeclarante());
                    existing.setRazaoSocial(dto.getRazaoSocial());
                    existing.setNomeResponsavel(dto.getNomeResponsavel());
                    existing.setFoneResponsavel(dto.getFoneResponsavel());
                    existing.setEmailResponsavel(dto.getEmailResponsavel());
                    return existing;
                })
                .orElseGet(() -> Declarante.builder()
                        .cnpjRaiz(dto.getCnpjRaiz())
                        .ieContribuinteDeclarante(dto.getIeContribuinteDeclarante())
                        .razaoSocial(dto.getRazaoSocial())
                        .nomeResponsavel(dto.getNomeResponsavel())
                        .foneResponsavel(dto.getFoneResponsavel())
                        .emailResponsavel(dto.getEmailResponsavel())
                        .build());
        Declarante salvo = repository.save(entidade);
        return toDTO(salvo);
    }

    private static DeclaranteDTO toDTO(Declarante e) {
        return DeclaranteDTO.builder()
                .id(e.getId())
                .cnpjRaiz(e.getCnpjRaiz())
                .ieContribuinteDeclarante(e.getIeContribuinteDeclarante())
                .razaoSocial(e.getRazaoSocial())
                .nomeResponsavel(e.getNomeResponsavel())
                .foneResponsavel(e.getFoneResponsavel())
                .emailResponsavel(e.getEmailResponsavel())
                .build();
    }
}
