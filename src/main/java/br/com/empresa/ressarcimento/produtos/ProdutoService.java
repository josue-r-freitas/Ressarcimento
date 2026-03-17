package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.planilhas.LeitorPlanilhaProdutos;
import br.com.empresa.ressarcimento.planilhas.dto.ProdutoPlanilhaDTO;
import br.com.empresa.ressarcimento.produtos.api.ProdutoDTO;
import br.com.empresa.ressarcimento.produtos.api.ArquivoProdutosDTO;
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
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoMatrizRepository produtoRepository;
    private final ArquivoProdutosRepository arquivoRepository;
    private final LeitorPlanilhaProdutos leitorPlanilha;
    private final DeclaranteService declaranteService;
    private final GeradorXmlProdutos geradorXml;

    @Transactional
    public ResultadoImportacaoDTO importar(MultipartFile arquivo) throws IOException {
        String nome = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename().toLowerCase() : "";
        List<ProdutoPlanilhaDTO> linhas;
        try (InputStream is = arquivo.getInputStream()) {
            if (nome.endsWith(".csv")) {
                linhas = leitorPlanilha.lerCsv(is);
            } else {
                linhas = leitorPlanilha.lerExcel(is);
            }
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
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        List<ProdutoMatriz> produtos = produtoRepository.findAllByOrderByCodInternoProduto();
        String xml = geradorXml.gerar(declarante, produtos);
        ArquivoProdutos arquivo = ArquivoProdutos.builder()
                .declarante(declarante)
                .dataGeracao(LocalDateTime.now())
                .status("GERADO")
                .xmlContent(xml)
                .build();
        arquivoRepository.save(arquivo);
        return xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
