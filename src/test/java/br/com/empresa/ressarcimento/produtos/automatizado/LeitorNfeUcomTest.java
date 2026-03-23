package br.com.empresa.ressarcimento.produtos.automatizado;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LeitorNfeUcomTest {

    private static final String CHAVE = "12345678901234567890123456789012345678901234";

    private final LeitorNfeUcom leitor = new LeitorNfeUcom();

    @Test
    void extraiUcom_porNItem_eCProd(@TempDir Path dir) throws Exception {
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <NFe>
                    <infNFe Id="NFe%s">
                      <det nItem="1">
                        <prod>
                          <cProd>P001</cProd>
                          <uCom>UN</uCom>
                        </prod>
                      </det>
                    </infNFe>
                  </NFe>
                </nfeProc>
                """
                        .formatted(CHAVE);
        Path xmlPath = dir.resolve(CHAVE + ".xml");
        Files.writeString(xmlPath, xml);

        assertThat(leitor.localizarArquivoXml(dir, CHAVE)).contains(xmlPath);
        assertThat(leitor.extrairUcom(xmlPath, 1, "P001")).contains("UN");
    }
}
