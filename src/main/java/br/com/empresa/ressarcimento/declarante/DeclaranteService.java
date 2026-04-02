package br.com.empresa.ressarcimento.declarante;

import br.com.empresa.ressarcimento.declarante.api.DeclaranteDTO;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.shared.exception.DeclaranteNaoEncontradoException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeclaranteService {

    private final DeclaranteRepository repository;

    @Transactional(readOnly = true)
    public DeclaranteDTO buscar() {
        Declarante entidade = repository.findFirstByOrderByIdAsc()
                .orElseThrow(DeclaranteNaoEncontradoException::new);
        return toDTO(entidade);
    }

    /** Para telas que precisam pré-preencher o formulário sem lançar se ainda não houver declarante. */
    @Transactional(readOnly = true)
    public Optional<DeclaranteDTO> buscarSeExistir() {
        return repository.findFirstByOrderByIdAsc().map(DeclaranteService::toDTO);
    }

    @Transactional(readOnly = true)
    public Declarante getEntidadeOuLanca() {
        return repository.findFirstByOrderByIdAsc()
                .orElseThrow(DeclaranteNaoEncontradoException::new);
    }

    /**
     * Persiste o único declarante do sistema. Atualiza sempre o registro de menor {@code id} se já existir
     * algum — não localiza por CNPJ, pois {@code CHAR(8)} / diferenças de digitação geravam INSERT duplicado ou
     * falha de unique e a tela voltava sem refletir os dados enviados.
     */
    @Transactional
    public DeclaranteDTO salvar(DeclaranteDTO dto) {
        normalizarCamposTexto(dto);
        Declarante entidade = repository.findFirstByOrderByIdAsc()
                .map(existing -> {
                    existing.setCnpjRaiz(dto.getCnpjRaiz());
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
                .cnpjRaiz(trim(e.getCnpjRaiz()))
                .ieContribuinteDeclarante(trim(e.getIeContribuinteDeclarante()))
                .razaoSocial(trim(e.getRazaoSocial()))
                .nomeResponsavel(trim(e.getNomeResponsavel()))
                .foneResponsavel(trim(e.getFoneResponsavel()))
                .emailResponsavel(trim(e.getEmailResponsavel()))
                .build();
    }

    /**
     * Remove espaços à direita/esquerda. O CNPJ raiz é {@code CHAR(8)} no SQL Server; o driver pode
     * devolver padding, o que quebra {@code @Pattern(\\d{8})} no POST do formulário.
     */
    private static void normalizarCamposTexto(DeclaranteDTO dto) {
        if (dto.getCnpjRaiz() != null) {
            dto.setCnpjRaiz(dto.getCnpjRaiz().trim());
        }
        if (dto.getIeContribuinteDeclarante() != null) {
            dto.setIeContribuinteDeclarante(dto.getIeContribuinteDeclarante().trim());
        }
        if (dto.getRazaoSocial() != null) {
            dto.setRazaoSocial(dto.getRazaoSocial().trim());
        }
        if (dto.getNomeResponsavel() != null) {
            dto.setNomeResponsavel(dto.getNomeResponsavel().trim());
        }
        if (dto.getFoneResponsavel() != null) {
            dto.setFoneResponsavel(dto.getFoneResponsavel().trim());
        }
        if (dto.getEmailResponsavel() != null) {
            dto.setEmailResponsavel(dto.getEmailResponsavel().trim());
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
