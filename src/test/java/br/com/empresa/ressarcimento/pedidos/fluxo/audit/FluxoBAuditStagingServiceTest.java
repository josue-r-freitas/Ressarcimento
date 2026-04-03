package br.com.empresa.ressarcimento.pedidos.fluxo.audit;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import br.com.empresa.ressarcimento.produtos.automatizado.LeitorNfeUcom;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(FluxoBAuditStagingService.class)
class FluxoBAuditStagingServiceTest {

    @Autowired
    private FluxoBAuditStagingService stagingService;

    @Autowired
    private FluxoBAuditNfeEntradaRepository nfeEntradaRepository;

    @Autowired
    private FluxoBAuditItemNfeEntradaRepository itemEntradaRepository;

    @Autowired
    private FluxoBAuditNfeSaidaRepository nfeSaidaRepository;

    @Autowired
    private FluxoBAuditItemNfeSaidaRepository itemSaidaRepository;

    @Test
    void limparStaging_removeTodasLinhas() {
        nfeSaidaRepository.save(FluxoBAuditNfeSaida.builder()
                .chaveNFe("12345678901234567890123456789012345678901234")
                .statusProcessamento("SEM_XML")
                .build());
        assertThat(nfeSaidaRepository.count()).isEqualTo(1);

        stagingService.limparStaging();

        assertThat(nfeSaidaRepository.count()).isZero();
        assertThat(itemSaidaRepository.count()).isZero();
        assertThat(nfeEntradaRepository.count()).isZero();
        assertThat(itemEntradaRepository.count()).isZero();
    }

    @Test
    void persistirEntradasDoResumo_semXml_gravaCabecalhoEItens() throws Exception {
        String chave = "22345678901234567890123456789012345678901234";
        List<ResumoNfLinhaDTO> linhas = List.of(ResumoNfLinhaDTO.builder()
                .numeroLinhaPlanilha(2)
                .chave(chave)
                .seqItem(1)
                .codgItem("P001")
                .cnpjFornecedor("12345678000199")
                .dataApresentacao(LocalDate.of(2024, 3, 10))
                .tributo("1380")
                .nrNota("12345")
                .qtdUnitCompra(new BigDecimal("10.5"))
                .valorUnitario(new BigDecimal("2.25"))
                .cfop("1102")
                .valorImposto(new BigDecimal("1.00"))
                .build());

        stagingService.persistirEntradasDoResumo(linhas, Path.of("nfe-inexistente"), new LeitorNfeUcom());

        assertThat(nfeEntradaRepository.findAll()).hasSize(1);
        FluxoBAuditNfeEntrada h = nfeEntradaRepository.findAll().get(0);
        assertThat(h.getChaveNFe()).isEqualTo(chave);
        assertThat(h.getNrNota()).isEqualTo("12345");
        assertThat(h.getDhEmi()).isNull();
        assertThat(itemEntradaRepository.findAll()).hasSize(1);
        assertThat(itemEntradaRepository.findAll().get(0).getCfop()).isEqualTo("1102");
        assertThat(itemEntradaRepository.findAll().get(0).getQtdUnitCompra()).isEqualByComparingTo("10.5");
    }
}
