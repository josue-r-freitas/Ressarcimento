package br.com.empresa.ressarcimento.manutencao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.ressarcimento.declarante.DeclaranteRepository;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoRepository;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
import br.com.empresa.ressarcimento.produtos.ProdutoMatrizRepository;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ManutencaoDadosServiceIntegrationTest {

    @Autowired
    private ManutencaoDadosService manutencaoDadosService;

    @Autowired
    private DeclaranteRepository declaranteRepository;

    @Autowired
    private ProdutoMatrizRepository produtoMatrizRepository;

    @Autowired
    private ProcessamentoRessarcimentoRepository processamentoRessarcimentoRepository;

    @Test
    void limpar_removeMatrizMantemDeclarante() {
        Declarante d = declaranteRepository.save(Declarante.builder()
                .cnpjRaiz("12345678")
                .ieContribuinteDeclarante("123456789")
                .razaoSocial("Empresa Teste Ltda")
                .nomeResponsavel("Fulano Silva")
                .foneResponsavel("11999999999")
                .emailResponsavel("a@b.com")
                .build());
        ProcessamentoRessarcimento proc = processamentoRessarcimentoRepository.save(ProcessamentoRessarcimento.builder()
                .declarante(d)
                .anoReferencia("2024")
                .mesReferencia("01")
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(ProcessamentoRessarcimento.STATUS_EM_ANDAMENTO)
                .build());
        produtoMatrizRepository.save(ProdutoMatriz.builder()
                .codInternoProduto("P1")
                .descricaoProduto("Um")
                .unidadeInternaProduto("UN")
                .fatorConversao(BigDecimal.ONE)
                .cnpjFornecedor("12345678901234")
                .codProdFornecedor("F1")
                .unidadeProdutoFornecedor("UN")
                .processamentoRessarcimento(proc)
                .build());

        assertThat(produtoMatrizRepository.count()).isPositive();

        manutencaoDadosService.limparTudoExcetoDeclarante();

        assertThat(declaranteRepository.findById(d.getId())).isPresent();
        assertThat(produtoMatrizRepository.count()).isZero();
    }
}
