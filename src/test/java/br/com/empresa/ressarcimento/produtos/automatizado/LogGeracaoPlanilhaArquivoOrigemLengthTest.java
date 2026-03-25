package br.com.empresa.ressarcimento.produtos.automatizado;

import br.com.empresa.ressarcimento.produtos.automatizado.domain.LogGeracaoPlanilha;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
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
                .build();
        repository.saveAndFlush(row);
        Assertions.assertThat(repository.findAll()).hasSize(1);
    }
}
