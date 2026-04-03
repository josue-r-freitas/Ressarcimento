package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.pedidos.ItemNotaSaidaRepository;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditStagingService;
import br.com.empresa.ressarcimento.planilhas.LeitorPlanilhaProdutos;
import br.com.empresa.ressarcimento.planilhas.dto.ProdutoPlanilhaDTO;
import br.com.empresa.ressarcimento.produtos.api.ProdutoDTO;
import br.com.empresa.ressarcimento.produtos.api.ArquivoProdutosDTO;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoRepository;
import br.com.empresa.ressarcimento.produtos.domain.ArquivoProdutos;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import br.com.empresa.ressarcimento.shared.api.ErroPlanilhaDTO;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import br.com.empresa.ressarcimento.xml.produto.GeradorXmlProdutos;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoMatrizRepository produtoRepository;
    private final ArquivoProdutosRepository arquivoRepository;
    private final ItemNotaSaidaRepository itemNotaSaidaRepository;
    private final LeitorPlanilhaProdutos leitorPlanilha;
    private final DeclaranteService declaranteService;
    private final GeradorXmlProdutos geradorXml;
    private final ProcessamentoRessarcimentoRepository processamentoRessarcimentoRepository;
    private final FluxoBAuditStagingService fluxoBAuditStagingService;

    @Transactional
    public ResultadoImportacaoDTO importar(MultipartFile arquivo) throws IOException {
        String nome = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "";
        try (InputStream is = arquivo.getInputStream()) {
            return importar(is, nome);
        }
    }

    /** Importação a partir de fluxo programático (ex.: pipeline Processar Ressarcimento). */
    @Transactional
    public ResultadoImportacaoDTO importar(InputStream inputStream, String nomeArquivoOriginal) throws IOException {
        String nome = nomeArquivoOriginal != null ? nomeArquivoOriginal.toLowerCase() : "";
        List<ProdutoPlanilhaDTO> linhas;
        if (nome.endsWith(".csv")) {
            linhas = leitorPlanilha.lerCsv(inputStream);
        } else {
            linhas = leitorPlanilha.lerExcel(inputStream);
        }
        List<ErroPlanilhaDTO> erros = new ArrayList<>();
        List<ProdutoMatriz> aPersistir = new ArrayList<>();
        for (ProdutoPlanilhaDTO dto : linhas) {
            var violations = ValidacaoPlanilhaUtil.validar(dto);
            if (!violations.isEmpty()) {
                for (String msg : violations) {
                    erros.add(ErroPlanilhaDTO.builder()
                            .linha(dto.getNumeroLinha())
                            .campo("linha")
                            .valorInformado(toString(dto))
                            .mensagem(msg)
                            .build());
                }
                continue;
            }
            if (!StringUtils.hasText(dto.getUnidadeProdutoFornecedor())) {
                erros.add(ErroPlanilhaDTO.builder()
                        .linha(dto.getNumeroLinha())
                        .campo("unidade_fornecedor")
                        .valorInformado("")
                        .mensagem(
                                "Unidade do fornecedor é obrigatória para importação no cadastro. "
                                        + "Preencha manualmente se a geração automática deixou em branco (uCom ausente na NF-e).")
                        .build());
                continue;
            }
            ProdutoMatriz entidade = toEntity(dto);
            aPersistir.add(entidade);
        }
        if (!erros.isEmpty()) {
            return ResultadoImportacaoDTO.builder()
                    .totalLinhasProcessadas(linhas.size())
                    .totalLinhasComErro(erros.size())
                    .totalPersistidas(0)
                    .erros(erros)
                    .build();
        }
        // Staging do Fluxo B referencia produto_matriz; sem limpar, deleteAllInBatch falha (FK).
        fluxoBAuditStagingService.limparStaging();
        itemNotaSaidaRepository.desvincularProdutosMatriz();
        produtoRepository.deleteAllInBatch();
        for (ProdutoMatriz p : aPersistir) {
            produtoRepository.save(p);
        }
        return ResultadoImportacaoDTO.builder()
                .totalLinhasProcessadas(linhas.size())
                .totalLinhasComErro(0)
                .totalPersistidas(aPersistir.size())
                .erros(List.of())
                .build();
    }

    private static String toString(ProdutoPlanilhaDTO dto) {
        return dto.getCodInternoProduto() + "|" + dto.getDescricaoProduto();
    }

    private static ProdutoMatriz toEntity(ProdutoPlanilhaDTO dto) {
        return ProdutoMatriz.builder()
                .codInternoProduto(dto.getCodInternoProduto())
                .descricaoProduto(dto.getDescricaoProduto())
                .unidadeInternaProduto(dto.getUnidadeInternaProduto())
                .fatorConversao(dto.getFatorConversao())
                .cnpjFornecedor(dto.getCnpjFornecedor())
                .codProdFornecedor(dto.getCodProdFornecedor())
                .unidadeProdutoFornecedor(dto.getUnidadeProdutoFornecedor())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ProdutoDTO> listar(Pageable pageable, String codigo, String descricao) {
        if ((codigo != null && !codigo.isBlank()) || (descricao != null && !descricao.isBlank())) {
            return produtoRepository
                    .findByCodInternoProdutoContainingOrDescricaoProdutoContaining(
                            codigo != null ? codigo : "", descricao != null ? descricao : "", pageable)
                    .map(this::toDTO);
        }
        return produtoRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional
    public byte[] gerarXml() throws JAXBException {
        return gerarXml(null);
    }

    @Transactional
    public byte[] gerarXml(Long processamentoRessarcimentoId) throws JAXBException {
        ArquivoProdutos salvo = persistirGeracaoXml(processamentoRessarcimentoId);
        return salvo.getXmlContent().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Persiste o XML de produtos e devolve o id do registo em {@code arquivo_produtos} (ex.: pós-pipeline). */
    @Transactional
    public Long gerarXmlRetornandoIdArquivo(Long processamentoRessarcimentoId) throws JAXBException {
        return persistirGeracaoXml(processamentoRessarcimentoId).getId();
    }

    private ArquivoProdutos persistirGeracaoXml(Long processamentoRessarcimentoId) throws JAXBException {
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        List<String> codigosEmNotas;
        if (processamentoRessarcimentoId != null) {
            codigosEmNotas = itemNotaSaidaRepository.findDistinctCodInternoProdutoByNotaSaidaDeclaranteIdAndProcessamentoId(
                    declarante.getId(), processamentoRessarcimentoId);
        } else {
            codigosEmNotas =
                    itemNotaSaidaRepository.findDistinctCodInternoProdutoByNotaSaidaDeclaranteId(declarante.getId());
        }
        if (codigosEmNotas.isEmpty()) {
            if (processamentoRessarcimentoId != null) {
                throw new IllegalArgumentException(
                        "Não há códigos de produto em itens de notas de saída deste processamento "
                                + "(nota_saida.processamento_ressarcimento_id = "
                                + processamentoRessarcimentoId
                                + "). Verifique se as NF-e de saída foram gravadas para este processamento.");
            }
            throw new IllegalArgumentException(
                    "Não há códigos de produto em itens de notas de saída (item_nota_saida) para este declarante. "
                            + "Importe ou gere as operações de pedidos (nota_saida / item_nota_saida) antes de gerar o XML de produtos.");
        }
        List<ProdutoMatriz> candidatos =
                produtoRepository.findByCodInternoProdutoInOrderByCodInternoProduto(codigosEmNotas);
        Map<String, ProdutoMatriz> umPorCodigo = new LinkedHashMap<>();
        for (ProdutoMatriz p : candidatos) {
            umPorCodigo.putIfAbsent(p.getCodInternoProduto(), p);
        }
        List<String> semMatriz = codigosEmNotas.stream()
                .filter(c -> !umPorCodigo.containsKey(c))
                .sorted()
                .distinct()
                .toList();
        if (!semMatriz.isEmpty()) {
            throw new IllegalArgumentException(
                    "Existem códigos em item_nota_saida sem produto correspondente na matriz: "
                            + String.join(", ", semMatriz));
        }
        List<ProdutoMatriz> produtos = codigosEmNotas.stream()
                .sorted()
                .distinct()
                .map(umPorCodigo::get)
                .toList();
        String xml = geradorXml.gerar(declarante, produtos);
        ArquivoProdutos.ArquivoProdutosBuilder b = ArquivoProdutos.builder()
                .declarante(declarante)
                .dataGeracao(LocalDateTime.now())
                .status("GERADO")
                .xmlContent(xml);
        if (processamentoRessarcimentoId != null) {
            b.processamentoRessarcimento(
                    processamentoRessarcimentoRepository.getReferenceById(processamentoRessarcimentoId));
        }
        return arquivoRepository.save(b.build());
    }

    @Transactional(readOnly = true)
    public List<ArquivoProdutosDTO> listarHistorico(Pageable pageable) {
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        return arquivoRepository.findByDeclaranteIdOrderByDataGeracaoDesc(declarante.getId(), pageable)
                .stream()
                .map(this::toArquivoDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] downloadXmlHistorico(Long id) {
        ArquivoProdutos arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Arquivo de produtos não encontrado: " + id));
        String xml = arquivo.getXmlContent();
        if (xml == null) throw new IllegalArgumentException("Conteúdo XML não disponível para este registro.");
        return xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private ProdutoDTO toDTO(ProdutoMatriz p) {
        return ProdutoDTO.builder()
                .id(p.getId())
                .codInternoProduto(p.getCodInternoProduto())
                .descricaoProduto(p.getDescricaoProduto())
                .unidadeInternaProduto(p.getUnidadeInternaProduto())
                .fatorConversao(p.getFatorConversao())
                .cnpjFornecedor(p.getCnpjFornecedor())
                .codProdFornecedor(p.getCodProdFornecedor())
                .unidadeProdutoFornecedor(p.getUnidadeProdutoFornecedor())
                .build();
    }

    private ArquivoProdutosDTO toArquivoDTO(ArquivoProdutos a) {
        return ArquivoProdutosDTO.builder()
                .id(a.getId())
                .dataGeracao(a.getDataGeracao())
                .status(a.getStatus())
                .mensagemLog(a.getMensagemLog())
                .build();
    }
}
