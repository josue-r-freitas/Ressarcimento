package br.com.empresa.ressarcimento.declarante;

import br.com.empresa.ressarcimento.declarante.api.DeclaranteDTO;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.shared.exception.DeclaranteNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeclaranteServiceTest {

    @Mock
    private DeclaranteRepository repository;

    @InjectMocks
    private DeclaranteService service;

    @Test
    void buscar_lancaQuandoNaoExisteDeclarante() {
        when(repository.findAll()).thenReturn(List.of());
        assertThatThrownBy(() -> service.buscar())
                .isInstanceOf(DeclaranteNaoEncontradoException.class);
    }

    @Test
    void buscarSeExistir_retornaVazioQuandoNaoHaDeclarante() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.buscarSeExistir()).isEmpty();
    }

    @Test
    void buscarSeExistir_retornaDtoQuandoExiste() {
        Declarante entidade = Declarante.builder()
                .id(1L)
                .cnpjRaiz("12345678")
                .ieContribuinteDeclarante("12345678")
                .razaoSocial("Empresa Teste")
                .nomeResponsavel("João")
                .foneResponsavel("92999999999")
                .emailResponsavel("joao@teste.com")
                .build();
        when(repository.findAll()).thenReturn(List.of(entidade));
        assertThat(service.buscarSeExistir()).isPresent().get().satisfies(d -> assertThat(d.getCnpjRaiz()).isEqualTo("12345678"));
    }

    @Test
    void buscar_retornaDTOQuandoExiste() {
        Declarante entidade = Declarante.builder()
                .id(1L)
                .cnpjRaiz("12345678")
                .ieContribuinteDeclarante("12345678")
                .razaoSocial("Empresa Teste")
                .nomeResponsavel("João")
                .foneResponsavel("92999999999")
                .emailResponsavel("joao@teste.com")
                .build();
        when(repository.findAll()).thenReturn(List.of(entidade));

        DeclaranteDTO dto = service.buscar();

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCnpjRaiz()).isEqualTo("12345678");
        assertThat(dto.getRazaoSocial()).isEqualTo("Empresa Teste");
    }

    @Test
    void salvar_criaNovoQuandoCnpjRaizNaoExiste() {
        DeclaranteDTO dto = DeclaranteDTO.builder()
                .cnpjRaiz("87654321")
                .ieContribuinteDeclarante("87654321")
                .razaoSocial("Nova Empresa")
                .nomeResponsavel("Maria")
                .foneResponsavel("92988888888")
                .emailResponsavel("maria@teste.com")
                .build();
        when(repository.findByCnpjRaiz("87654321")).thenReturn(Optional.empty());
        when(repository.save(any(Declarante.class))).thenAnswer(inv -> {
            Declarante d = inv.getArgument(0);
            d.setId(2L);
            return d;
        });

        DeclaranteDTO salvo = service.salvar(dto);

        assertThat(salvo.getCnpjRaiz()).isEqualTo("87654321");
        assertThat(salvo.getRazaoSocial()).isEqualTo("Nova Empresa");
        verify(repository).save(any(Declarante.class));
    }

    @Test
    void salvar_atualizaQuandoCnpjRaizJaExiste() {
        Declarante existente = Declarante.builder()
                .id(1L)
                .cnpjRaiz("12345678")
                .ieContribuinteDeclarante("12345678")
                .razaoSocial("Antiga")
                .nomeResponsavel("João")
                .foneResponsavel("92999999999")
                .emailResponsavel("joao@teste.com")
                .build();
        DeclaranteDTO dto = DeclaranteDTO.builder()
                .cnpjRaiz("12345678")
                .ieContribuinteDeclarante("12345678")
                .razaoSocial("Razão Atualizada")
                .nomeResponsavel("João Silva")
                .foneResponsavel("92977777777")
                .emailResponsavel("joao.silva@teste.com")
                .build();
        when(repository.findByCnpjRaiz("12345678")).thenReturn(Optional.of(existente));
        when(repository.save(any(Declarante.class))).thenAnswer(inv -> inv.getArgument(0));

        DeclaranteDTO salvo = service.salvar(dto);

        assertThat(salvo.getRazaoSocial()).isEqualTo("Razão Atualizada");
        assertThat(existente.getRazaoSocial()).isEqualTo("Razão Atualizada");
        verify(repository).save(existente);
    }
}
