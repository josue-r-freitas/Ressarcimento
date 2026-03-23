package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParserEfdServiceTest {

    private static final String CHAVE = "12345678901234567890123456789012345678901234";

    private final ParserEfdService parser = new ParserEfdService();

    @Test
    void carregaC100C170_0200_0220(@TempDir Path dir) throws Exception {
        String conteudo =
                """
                |0200|ITEM01|Produto integracao||
                |0220|CX|2,500000|
                |C100|0|0|FORN|55|00|1|999|%s|
                |C170|1|ITEM01|X|10|UN|
                """
                        .formatted(CHAVE);
        Files.writeString(dir.resolve("efd.txt"), conteudo, java.nio.charset.StandardCharsets.ISO_8859_1);

        EfdIndice indice = parser.carregarDiretorio(dir);

        assertThat(indice.notaPorChave(CHAVE)).isPresent();
        assertThat(indice.notaPorChave(CHAVE).flatMap(n -> n.findItem(1)))
                .hasValueSatisfying(c -> {
                    assertThat(c.codItem()).isEqualTo("ITEM01");
                    assertThat(c.unid()).isEqualTo("UN");
                });
        assertThat(indice.infoItem("ITEM01")).hasValueSatisfying(i -> {
            assertThat(i.getDescrItem()).isEqualTo("Produto integracao");
            assertThat(i.getFatorConversao0220()).isEqualByComparingTo(new BigDecimal("2.5"));
        });
    }
}
