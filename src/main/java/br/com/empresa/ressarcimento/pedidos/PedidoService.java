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
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoLifecycle;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
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
import java.util.Optional;
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
    private final ProcessamentoRessarcimentoLifecycle processamentoRessarcimentoLifecycle;

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
        int anoProc = Integer.parseInt(anoRef.trim());
        int mesProc = Integer.parseInt(mesRef.trim());
        ProcessamentoRessarcimento proc =
                processamentoRessarcimentoLifecycle.iniciarEmAndamento(anoProc, mesProc);

        Map<String, NotaSaida> notasPorChave = new LinkedHashMap<>();
        for (OperacaoPlanilhaDTO dto : linhas) {
            NotaSaida nota = notasPorChave.computeIfAbsent(dto.getChaveNfeSaida(), chave -> {
                Optional<NotaSaida> existente = notaSaidaRepository.findByChaveNFe(chave);
                if (existente.isPresent()) {
                    NotaSaida n = existente.get();
                    ProcessamentoRessarcimento procAtual = n.getProcessamentoRessarcimento();
                    Long procId = procAtual == null ? null : procAtual.getId();
                    if (!proc.getId().equals(procId)) {
                        n.setProcessamentoRessarcimento(proc);
                        return notaSaidaRepository.save(n);
                    }
                    return n;
                }
                NotaSaida n = NotaSaida.builder()
                        .declarante(declarante)
                        .chaveNFe(chave)
                        .anoPeriodoReferencia(dto.getAnoReferencia())
                        .mesPeriodoReferencia(dto.getMesReferencia())
                        .processamentoRessarcimento(proc)
                        .build();
                return notaSaidaRepository.save(n);
            });
            ProdutoMatriz produto = produtoMatrizRepository.findByCodInternoProduto(dto.getCodInternoProduto()).stream().findFirst().orElse(null);
            NotaEntrada notaEntrada = null;
            if (dto.getChaveNfeEntrada() != null && !dto.getChaveNfeEntrada().isBlank()) {
                String chaveNe = dto.getChaveNfeEntrada().trim();
                Optional<NotaEntrada> neOpt = notaEntradaRepository.findByChaveNFeEntrada(chaveNe);
                if (neOpt.isPresent()) {
                    notaEntrada = neOpt.get();
                    ProcessamentoRessarcimento procNe = notaEntrada.getProcessamentoRessarcimento();
                    Long procNeId = procNe == null ? null : procNe.getId();
                    if (!proc.getId().equals(procNeId)) {
                        notaEntrada.setProcessamentoRessarcimento(proc);
                        notaEntrada = notaEntradaRepository.save(notaEntrada);
                    }
                } else {
                    notaEntrada = notaEntradaRepository.save(NotaEntrada.builder()
                            .chaveNFeEntrada(chaveNe)
                            .chaveCTeEntrada(blankToNull(dto.getChaveCteEntrada()))
                            .chaveMDFeEntrada(blankToNull(dto.getChaveMdfeEntrada()))
                            .processamentoRessarcimento(proc)
                            .build());
                }
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

    /** Alinha com CHAR(2) no banco e com planilhas que usam 01–12. */
    private static String normalizarMesDoisDigitos(String mes) {
        if (mes == null) {
            return "";
        }
        String t = mes.trim();
        if (t.isEmpty()) {
            return t;
        }
        try {
            int m = Integer.parseInt(t);
            if (m >= 1 && m <= 9) {
                return "0" + m;
            }
            if (m >= 10 && m <= 12) {
                return String.valueOf(m);
            }
        } catch (NumberFormatException ignored) {
        }
        return t;
    }

    @Transactional(readOnly = true)
    public Page<NotaSaidaDTO> listar(Pageable pageable, String ano, String mes) {
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        if (ano != null && !ano.isBlank() && mes != null && !mes.isBlank()) {
            String anoT = ano.trim();
            String mesNorm = normalizarMesDoisDigitos(mes);
            Page<NotaSaida> page =
                    notaSaidaRepository.findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
                            declarante.getId(), anoT, mesNorm, pageable);
            if (page.isEmpty() && !mesNorm.equals(mes.trim())) {
                page = notaSaidaRepository.findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
                        declarante.getId(), anoT, mes.trim(), pageable);
            }
            return page.map(this::toDTO);
        }
        return notaSaidaRepository.findByDeclaranteId(declarante.getId(), pageable).map(this::toDTO);
    }

    @Transactional
    public byte[] gerarXml(String anoReferencia, String mesReferencia) throws JAXBException {
        Declarante declarante = declaranteService.getEntidadeOuLanca();
        String ano = anoReferencia != null ? anoReferencia.trim() : "";
        String mesNorm = normalizarMesDoisDigitos(mesReferencia);
        List<NotaSaida> notas =
                notaSaidaRepository.findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
                        declarante.getId(), ano, mesNorm);
        if (notas.isEmpty() && mesReferencia != null && !mesNorm.equals(mesReferencia.trim())) {
            notas = notaSaidaRepository.findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
                    declarante.getId(), ano, mesReferencia.trim());
        }
        if (notas.isEmpty()) {
            throw new IllegalArgumentException(
                    "Não há NF-e de saída com itens importados para o período "
                            + ano
                            + "/"
                            + mesNorm
                            + ". "
                            + "Em «Pedidos — importar», envie primeiro a planilha de operações com o mesmo ano/mês; "
                            + "use mês com dois dígitos (ex.: 01) se ainda não aparecer dados.");
        }
        ProcessamentoRessarcimento proc = notas.get(0).getProcessamentoRessarcimento();
        if (proc == null) {
            throw new IllegalStateException(
                    "Notas de saída do período sem processamento de ressarcimento vinculado; reimporte a planilha de operações.");
        }
        String xml = geradorXml.gerar(declarante, ano, mesNorm, notas);
        ArquivoPedido arquivo = ArquivoPedido.builder()
                .declarante(declarante)
                .anoReferencia(ano)
                .mesReferencia(mesNorm)
                .dataGeracao(LocalDateTime.now())
                .status("GERADO")
                .xmlContent(xml)
                .processamentoRessarcimento(proc)
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
