package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.pedidos.api.ArquivoPedidoDTO;
import br.com.empresa.ressarcimento.pedidos.api.NotaSaidaDTO;
import br.com.empresa.ressarcimento.pedidos.domain.ArquivoPedido;
import br.com.empresa.ressarcimento.pedidos.domain.ItemNotaSaida;
import br.com.empresa.ressarcimento.pedidos.domain.NotaEntrada;
import br.com.empresa.ressarcimento.pedidos.domain.NotaSaida;
import br.com.empresa.ressarcimento.planilhas.LeitorPlanilhaOperacoes;
import br.com.empresa.ressarcimento.planilhas.dto.OperacaoPlanilhaDTO;
import br.com.empresa.ressarcimento.produtos.ProdutoMatrizRepository;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import br.com.empresa.ressarcimento.shared.api.ErroPlanilhaDTO;
import br.com.empresa.ressarcimento.shared.api.ResultadoImportacaoDTO;
import br.com.empresa.ressarcimento.xml.pedido.GeradorXmlPedidos;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final NotaSaidaRepository notaSaidaRepository;
    private final NotaEntradaRepository notaEntradaRepository;
    private final ArquivoPedidoRepository arquivoPedidoRepository;
    private final ProdutoMatrizRepository produtoMatrizRepository;
    private final LeitorPlanilhaOperacoes leitorPlanilha;
    private final ValidacaoPlanilhaOperacoes validacaoPlanilha;
    private final DeclaranteService declaranteService;
    private final GeradorXmlPedidos geradorXml;

    @Transactional
    public ResultadoImportacaoDTO importar(MultipartFile arquivo) throws IOException {
        String nome = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename().toLowerCase() : "";
        List<OperacaoPlanilhaDTO> linhas;
        try (InputStream is = arquivo.getInputStream()) {
            if (nome.endsWith(".csv")) {
                linhas = leitorPlanilha.lerCsv(is);
            } else {
                linhas = leitorPlanilha.lerExcel(is);
            }
        }
        List<ErroPlanilhaDTO> erros = new ArrayList<>();
        for (OperacaoPlanilhaDTO dto : linhas) {
            for (String msg : validacaoPlanilha.validar(dto)) {
                erros.add(ErroPlanilhaDTO.builder()
                        .linha(dto.getNumeroLinha())
                        .campo("linha")
                        .valorInformado(dto.getChaveNfeSaida() + "|" + dto.getCodInternoProduto())
                        .mensagem(msg)
                        .build());
            }
        }
        if (!erros.isEmpty()) {
            return ResultadoImportacaoDTO.builder()
                    .totalLinhasProcessadas(linhas.size())
                    .totalLinhasComErro(erros.size())
                    .totalPersistidas(0)
                    .erros(erros)
                    .build();
        }
        String anoRef = linhas.get(0).getAnoReferencia();
        String mesRef = linhas.get(0).getMesReferencia();
        for (OperacaoPlanilhaDTO dto : linhas) {
            if (!dto.getAnoReferencia().equals(anoRef) || !dto.getMesReferencia().equals(mesRef)) {
                erros.add(ErroPlanilhaDTO.builder()
                        .linha(dto.getNumeroLinha())
                        .campo("ano_referencia/mes_referencia")
                        .valorInformado(dto.getAnoReferencia() + "/" + dto.getMesReferencia())
                        .mensagem("Todas as linhas devem ter o mesmo ano e mês de referência")
                        .build());
            }
        }
        if (!erros.isEmpty()) {
            return ResultadoImportacaoDTO.builder()
                    .totalLinhasProcessadas(linhas.size())
                    .totalLinhasComErro(erros.size())
                    .totalPersistidas(0)
                    .erros(erros)
                    .build();
        }

        Declarante declarante = declaranteService.getEntidadeOuLanca();
        Map<String, NotaSaida> notasPorChave = new LinkedHashMap<>();
        for (OperacaoPlanilhaDTO dto : linhas) {
            NotaSaida nota = notasPorChave.computeIfAbsent(dto.getChaveNfeSaida(), chave ->
                    notaSaidaRepository.findByChaveNFe(chave).orElseGet(() -> {
                        NotaSaida n = NotaSaida.builder()
                                .declarante(declarante)
                                .chaveNFe(chave)
                                .anoPeriodoReferencia(dto.getAnoReferencia())
                                .mesPeriodoReferencia(dto.getMesReferencia())
                                .build();
                        return notaSaidaRepository.save(n);
                    }));
            ProdutoMatriz produto = produtoMatrizRepository.findByCodInternoProduto(dto.getCodInternoProduto()).stream().findFirst().orElse(null);
            NotaEntrada notaEntrada = null;
            if (dto.getChaveNfeEntrada() != null && !dto.getChaveNfeEntrada().isBlank()) {
                notaEntrada = notaEntradaRepository.findByChaveNFeEntrada(dto.getChaveNfeEntrada())
                        .orElseGet(() -> {
                            NotaEntrada ne = NotaEntrada.builder()
                                    .chaveNFeEntrada(dto.getChaveNfeEntrada())
                                    .chaveCTeEntrada(blankToNull(dto.getChaveCteEntrada()))
                                    .chaveMDFeEntrada(blankToNull(dto.getChaveMdfeEntrada()))
                                    .build();
                            return notaEntradaRepository.save(ne);
                        });
            }
            int numItem = Integer.parseInt(dto.getNumItemNfe().trim());
            ItemNotaSaida item = ItemNotaSaida.builder()
                    .notaSaida(nota)
                    .produtoMatriz(produto)
                    .codInternoProduto(dto.getCodInternoProduto())
                    .numItemNFe(numItem)
                    .notaEntrada(notaEntrada)
                    .build();
            nota.getItens().add(item);
        }
        notaSaidaRepository.saveAll(notasPorChave.values());
        return ResultadoImportacaoDTO.builder()
                .totalLinhasProcessadas(linhas.size())
                .totalLinhasComErro(0)
                .totalPersistidas(linhas.size())
                .erros(List.of())
                .build();
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s;
    }

    @Transactional(readOnly = true)
    public Page<NotaSaidaDTO> listar(Pageable pageable, String ano, String mes) {
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        if (ano != null && !ano.isBlank() && mes != null && !mes.isBlank()) {
            return notaSaidaRepository.findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
                    declarante.getId(), ano, mes, pageable).map(this::toDTO);
        }
        return notaSaidaRepository.findByDeclaranteId(declarante.getId(), pageable).map(this::toDTO);
    }

    @Transactional
    public byte[] gerarXml(String anoReferencia, String mesReferencia) throws JAXBException {
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        List<NotaSaida> notas = notaSaidaRepository.findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
                declarante.getId(), anoReferencia, mesReferencia);
        String xml = geradorXml.gerar(declarante, anoReferencia, mesReferencia, notas);
        ArquivoPedido arquivo = ArquivoPedido.builder()
                .declarante(declarante)
                .anoReferencia(anoReferencia)
                .mesReferencia(mesReferencia)
                .dataGeracao(LocalDateTime.now())
                .status("GERADO")
                .xmlContent(xml)
                .build();
        arquivoPedidoRepository.save(arquivo);
        return xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<ArquivoPedidoDTO> listarHistorico(Pageable pageable) {
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        return arquivoPedidoRepository.findByDeclaranteIdOrderByDataGeracaoDesc(declarante.getId(), pageable)
                .stream().map(this::toArquivoDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] downloadXmlHistorico(Long id) {
        ArquivoPedido arquivo = arquivoPedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Arquivo de pedido não encontrado: " + id));
        String xml = arquivo.getXmlContent();
        if (xml == null) throw new IllegalArgumentException("Conteúdo XML não disponível para este registro.");
        return xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private NotaSaidaDTO toDTO(NotaSaida n) {
        return NotaSaidaDTO.builder()
                .id(n.getId())
                .chaveNFe(n.getChaveNFe())
                .anoPeriodoReferencia(n.getAnoPeriodoReferencia())
                .mesPeriodoReferencia(n.getMesPeriodoReferencia())
                .quantidadeItens(n.getItens() != null ? n.getItens().size() : 0)
                .build();
    }

    private ArquivoPedidoDTO toArquivoDTO(ArquivoPedido a) {
        return ArquivoPedidoDTO.builder()
                .id(a.getId())
                .anoReferencia(a.getAnoReferencia())
                .mesReferencia(a.getMesReferencia())
                .dataGeracao(a.getDataGeracao())
                .status(a.getStatus())
                .mensagemLog(a.getMensagemLog())
                .build();
    }
}
