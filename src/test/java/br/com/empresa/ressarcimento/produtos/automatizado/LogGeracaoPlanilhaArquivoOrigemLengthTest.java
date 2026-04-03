package br.com.empresa.ressarcimento.produtos.automatizado;

import br.com.empresa.ressarcimento.declarante.DeclaranteRepository;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoRepository;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import br.com.empresa.ressarcimento.produtos.automatizado.domain.LogGeracaoPlanilha;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Evidência: {@code arquivo_origem} é NVARCHAR(500); valores maiores geram erro ao persistir (500 na API).
 */
@DataJpaTest
@ActiveProfiles("test")
class LogGeracaoPlanilhaArquivoOrigemLengthTest {

    @Autowired
    private LogGeracaoPlanilhaRepository repository;

    @Autowired
    private DeclaranteRepository declaranteRepository;

    @Autowired
    private ProcessamentoRessarcimentoRepository processamentoRessarcimentoRepository;

    private ProcessamentoRessarcimento processamento;

    @BeforeEach
    void criarProcessamento() {
        Declarante d = declaranteRepository.save(Declarante.builder()
                .cnpjRaiz("12345678")
                .ieContribuinteDeclarante("123456789")
                .razaoSocial("Empresa Teste Ltda")
                .nomeResponsavel("Fulano Silva")
                .foneResponsavel("11999999999")
                .emailResponsavel("a@b.com")
                .build());
        processamento = processamentoRessarcimentoRepository.save(ProcessamentoRessarcimento.builder()
                .declarante(d)
                .anoReferencia("2026")
                .mesReferencia("01")
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO)
                .build());
    }

    @Test
    void save_comArquivoOrigemMaiorQue500_disparaErroIntegridade() {
        String longo = "x".repeat(501);
        LogGeracaoPlanilha row = LogGeracaoPlanilha.builder()
                .tipo("TESTE")
                .chaveNfe(null)
                .numItem(null)
                .dataProcessamento(LocalDateTime.now())
                .arquivoOrigem(longo)
                .mensagem("msg")
                .processamentoRessarcimento(processamento)
                .build();

        Assertions.assertThatThrownBy(() -> repository.saveAndFlush(row))
                .isInstanceOfAny(DataIntegrityViolationException.class, org.hibernate.exception.GenericJDBCException.class);
    }

    @Test
    void save_comArquivoOrigemExatamente500_sucesso() {
        String ok = "y".repeat(500);
        LogGeracaoPlanilha row = LogGeracaoPlanilha.builder()
                .tipo("TESTE")
                .chaveNfe(null)
                .numItem(null)
                .dataProcessamento(LocalDateTime.now())
                .arquivoOrigem(ok)
                .mensagem("msg")
                .processamentoRessarcimento(processamento)
                .build();
        repository.saveAndFlush(row);
        Assertions.assertThat(repository.findAll()).hasSize(1);
    }
}
