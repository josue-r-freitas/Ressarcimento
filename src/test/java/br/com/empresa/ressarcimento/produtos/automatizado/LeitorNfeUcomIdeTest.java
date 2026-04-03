package br.com.empresa.ressarcimento.produtos.automatizado;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeitorNfeUcomIdeTest {

    private final LeitorNfeUcom leitor = new LeitorNfeUcom();

    @Test
    void lerIdeCampos_extraiDhSaiEntDhEmiEDEmi(@TempDir Path dir) throws Exception {
        Path xml = dir.resolve("nfe.xml");
        Files.writeString(
                xml,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <NFe xmlns="http://www.portalfiscal.inf.br/nfe">
                  <infNFe Id="NFe123">
                    <ide>
                      <dhEmi>2024-01-15T10:00:00-04:00</dhEmi>
                      <dhSaiEnt>2024-01-16T08:30:00-04:00</dhSaiEnt>
                      <dEmi>2024-01-15</dEmi>
                    </ide>
                  </infNFe>
                </NFe>
                """);

        Optional<NfeIdeCampos> r = leitor.lerIdeCampos(xml);
        assertThat(r).isPresent();
        assertThat(r.get().dhEmi()).contains("2024-01-15T10:00:00");
        assertThat(r.get().dhSaiEnt()).contains("2024-01-16T08:30:00");
        assertThat(r.get().dEmi()).isEqualTo("2024-01-15");
    }
}
