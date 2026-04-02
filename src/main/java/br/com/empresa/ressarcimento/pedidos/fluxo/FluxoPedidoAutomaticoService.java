package br.com.empresa.ressarcimento.pedidos.fluxo;

import br.com.empresa.ressarcimento.config.RessarcimentoProperties;
import br.com.empresa.ressarcimento.declarante.DeclaranteService;
import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import br.com.empresa.ressarcimento.pedidos.ArquivoPedidoRepository;
import br.com.empresa.ressarcimento.pedidos.api.AuditoriaEntradaConsumidaDTO;
import br.com.empresa.ressarcimento.pedidos.api.AuditoriaProdutoVendidoDTO;
import br.com.empresa.ressarcimento.pedidos.api.ExecucaoFluxoPedidoResumoDTO;
import br.com.empresa.ressarcimento.pedidos.api.GerarPedidoAutomaticoResponse;
import br.com.empresa.ressarcimento.pedidos.api.RastreabilidadeFluxoDTO;
import br.com.empresa.ressarcimento.pedidos.domain.ArquivoPedido;
import br.com.empresa.ressarcimento.pedidos.domain.ItemNotaSaida;
import br.com.empresa.ressarcimento.pedidos.domain.NotaEntrada;
import br.com.empresa.ressarcimento.pedidos.domain.NotaSaida;
import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import br.com.empresa.ressarcimento.produtos.ProdutoMatrizRepository;
import br.com.empresa.ressarcimento.produtos.automatizado.ItemNfeCfop;
import br.com.empresa.ressarcimento.produtos.automatizado.LeitorNfeUcom;
import br.com.empresa.ressarcimento.produtos.automatizado.LeitorResumoNf;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.C170Linha;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.EfdIndice;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.ParserEfdService;
import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import br.com.empresa.ressarcimento.shared.exception.RecursoNaoEncontradoException;
import br.com.empresa.ressarcimento.xml.pedido.GeradorXmlPedidos;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FluxoPedidoAutomaticoService {

    private static final Logger log = LoggerFactory.getLogger(FluxoPedidoAutomaticoService.class);

    public static final String STATUS_EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String STATUS_CONCLUIDO = "CONCLUIDO";
    public static final String STATUS_CONCLUIDO_AVISOS = "CONCLUIDO_COM_AVISOS";
    public static final String STATUS_ERRO = "ERRO";

    private static final Set<String> CFOPS_FLUXO_B = Set.of("6102", "6108");

    private final RessarcimentoProperties properties;
    private final ParserEfdService parserEfdService;
    private final LeitorNfeUcom leitorNfeUcom;
    private final LeitorResumoNf leitorResumoNf;
    private final ProdutoMatrizRepository produtoMatrizRepository;
    private final DeclaranteService declaranteService;
    private final GeradorXmlPedidos geradorXmlPedidos;
    private final ExecucaoFluxoPedidoRepository execucaoRepository;
    private final ArquivoPedidoRepository arquivoPedidoRepository;

    @Transactional
    public GerarPedidoAutomaticoResponse gerarAutomatico(int ano, int mes) throws JAXBException, IOException {
        Declarante decl = declaranteService.getEntidadeOuLanca();
        String anoStr = String.format("%04d", ano);
        String mesStr = mes >= 1 && mes <= 9 ? "0" + mes : String.valueOf(mes);

        Path dirEfd = exigirDir(properties.getEfdsDir(), "ressarcimento.efds-dir");
        Path dirNfeSaida = exigirDir(properties.getNfesSaidaDir(), "ressarcimento.nfes-saida-dir");
        Path dirNfeEntrada = exigirDir(properties.getNfesDir(), "ressarcimento.nfes-dir");
        Path dirResumo = exigirDir(properties.getResumoNotasDir(), "ressarcimento.resumo-notas-dir");
        Path arquivoResumo = resolverPrimeiroXlsx(dirResumo);

        ExecucaoFluxoPedido exec = ExecucaoFluxoPedido.builder()
                .declarante(decl)
                .anoReferencia(anoStr)
                .mesReferencia(mesStr)
                .dataHoraInicio(LocalDateTime.now())
                .statusExecucao(STATUS_EM_ANDAMENTO)
                .arquivoEfdUtilizado(dirEfd.toString())
                .pastaNfesSaida(dirNfeSaida.toString())
                .pastaNfesEntrada(dirNfeEntrada.toString())
                .arquivoResumonf(arquivoResumo.toString())
                .build();
        exec = execucaoRepository.save(exec);
        Long execId = exec.getId();
        MDC.put("idExecucao", String.valueOf(execId));
        log.info("Início Fluxo B idExecucao={} período={}-{}", execId, anoStr, mesStr);

        List<String> avisos = new ArrayList<>();

        try {
            EfdIndice indice = parserEfdService.carregarDiretorio(dirEfd);
            List<String> chavesSaida = new ArrayList<>(indice.chavesSaidaNoMes(ano, mes));
            chavesSaida.sort(Comparator.comparing(
                                    (String k) -> indice.dataDocumentoSaida(k).orElse(null),
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(k -> k));
            if (chavesSaida.isEmpty()) {
                addLog(exec, "WARN", "LEITURA_EFD", "Nenhuma NF-e de saída modelo 55 no período na EFD.", null);
                avisos.add("Nenhuma chave de saída no período na EFD.");
            }

            List<ResumoNfLinhaDTO> linhasResumo;
            try (InputStream in = Files.newInputStream(arquivoResumo)) {
                linhasResumo = leitorResumoNf.lerExcel(in);
            }
            linhasResumo = linhasResumo.stream()
                    .filter(l -> l.getDataApresentacao() != null)
                    .filter(l -> l.getDataApresentacao().getYear() == ano
                            && l.getDataApresentacao().getMonthValue() == mes)
                    .filter(l -> !StringUtils.hasText(l.getTributo()) || "1380".equals(l.getTributo().trim()))
                    .toList();

            Map<String, ArrayDeque<EntradaFifoSlot>> estoqueFifo =
                    construirEstoqueFifo(linhasResumo, indice, exec, avisos);

            List<NotaSaida> notasMontadas = new ArrayList<>();

            for (String chaveSaida : chavesSaida) {
                Optional<Path> xmlOpt = leitorNfeUcom.localizarArquivoXml(dirNfeSaida, chaveSaida);
                if (xmlOpt.isEmpty()) {
                    String msg = "XML de NF-e de saída não encontrado para chave " + chaveSaida;
                    avisos.add(msg);
                    addLog(exec, "WARN", "LEITURA_XML_SAIDA", msg, chaveSaida);
                    continue;
                }
                List<ItemNfeCfop> itensXml;
                try {
                    itensXml = leitorNfeUcom.listarItensComCfops(xmlOpt.get(), CFOPS_FLUXO_B);
                } catch (Exception e) {
                    avisos.add("Falha ao ler XML saída " + chaveSaida + ": " + e.getMessage());
                    addLog(exec, "ERROR", "LEITURA_XML_SAIDA", e.getMessage(), chaveSaida);
                    continue;
                }
                if (itensXml.isEmpty()) {
                    continue;
                }

                NotaSaida ns = NotaSaida.builder()
                        .declarante(decl)
                        .chaveNFe(chaveSaida)
                        .anoPeriodoReferencia(anoStr)
                        .mesPeriodoReferencia(mesStr)
                        .itens(new ArrayList<>())
                        .build();

                for (ItemNfeCfop ix : itensXml) {
                    Optional<ProdutoMatriz> pmOpt = resolverProdutoPorCProdSaida(ix.cProd());
                    if (pmOpt.isEmpty()) {
                        String msg = "Sem mapeamento de produto para cProd=" + ix.cProd() + " na saída " + chaveSaida;
                        avisos.add(msg);
                        addLog(exec, "WARN", "MAPEAMENTO_PRODUTO", msg, null);
                        continue;
                    }
                    ProdutoMatriz pm = pmOpt.get();
                    String codInt = pm.getCodInternoProduto();

                    BigDecimal qVenda = converterQuantidadeVenda(ix, pm);
                    ResultadoConsumoFifo consumo = consumirFifo(qVenda, codInt, estoqueFifo);

                    ItemNotaSaida item = ItemNotaSaida.builder()
                            .notaSaida(ns)
                            .produtoMatriz(pm)
                            .codInternoProduto(codInt)
                            .numItemNFe(ix.nItem())
                            .notaEntrada(consumo.primeiraEntrada())
                            .build();
                    item.getChavesNfeEntradaConsumidas().addAll(consumo.chaves());
                    ns.getItens().add(item);

                    AuditoriaProdutoVendido aud = AuditoriaProdutoVendido.builder()
                            .execucao(exec)
                            .codInternoProduto(codInt)
                            .chaveNfeSaida(chaveSaida)
                            .numItemNfe(ix.nItem())
                            .cfopItem(ix.cfop())
                            .quantidadeVendidaUnidadeInterna(qVenda)
                            .unidadeInterna(pm.getUnidadeInternaProduto())
                            .quantidadeTotalComprasConvertida(consumo.totalComprasInterno())
                            .suficiente(consumo.suficiente())
                            .build();
                    for (AuditoriaEntradaConsumida lin : consumo.linhasAuditoria()) {
                        lin.setAuditoriaProduto(aud);
                        aud.getEntradasConsumidas().add(lin);
                    }
                    exec.getAuditoriasProduto().add(aud);
                }

                if (!ns.getItens().isEmpty()) {
                    notasMontadas.add(ns);
                }
            }

            if (notasMontadas.isEmpty()) {
                exec.setStatusExecucao(STATUS_ERRO);
                exec.setDataHoraFim(LocalDateTime.now());
                addLog(exec, "ERROR", "GERACAO_XML", "Nenhuma operação com itens CFOP 6102/6108 elegível.", null);
                execucaoRepository.save(exec);
                throw new IllegalArgumentException(
                        "Fluxo B: nenhuma NF-e de saída com itens elegíveis (CFOP 6102/6108) e produto mapeado.");
            }

            String xml = geradorXmlPedidos.gerar(decl, anoStr, mesStr, notasMontadas);

            ArquivoPedido arq = ArquivoPedido.builder()
                    .declarante(decl)
                    .anoReferencia(anoStr)
                    .mesReferencia(mesStr)
                    .dataGeracao(LocalDateTime.now())
                    .status("GERADO_FLUXO_B")
                    .xmlContent(xml)
                    .execucaoFluxoPedido(exec)
                    .build();
            arq = arquivoPedidoRepository.save(arq);

            exec.setDataHoraFim(LocalDateTime.now());
            exec.setStatusExecucao(avisos.isEmpty() ? STATUS_CONCLUIDO : STATUS_CONCLUIDO_AVISOS);
            execucaoRepository.save(exec);

            log.info("Fim Fluxo B idExecucao={} status={}", execId, exec.getStatusExecucao());
            return GerarPedidoAutomaticoResponse.builder()
                    .idExecucao(execId)
                    .arquivoPedidoId(arq.getId())
                    .status(exec.getStatusExecucao())
                    .avisos(avisos)
                    .build();
        } catch (Exception e) {
            exec.setDataHoraFim(LocalDateTime.now());
            exec.setStatusExecucao(STATUS_ERRO);
            addLog(
                    exec,
                    "ERROR",
                    "GERAL",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    null);
            execucaoRepository.save(exec);
            throw e;
        } finally {
            MDC.remove("idExecucao");
        }
    }

    private void addLog(ExecucaoFluxoPedido exec, String nivel, String etapa, String mensagem, String detalhes) {
        LogExecucaoFluxo lg = LogExecucaoFluxo.builder()
                .execucao(exec)
                .nivel(nivel)
                .etapa(etapa)
                .mensagem(mensagem != null && mensagem.length() > 1000 ? mensagem.substring(0, 997) + "..." : mensagem)
                .detalhes(detalhes)
                .ts(LocalDateTime.now())
                .build();
        exec.getLogs().add(lg);
    }

    private Optional<ProdutoMatriz> resolverProdutoPorCProdSaida(String cProd) {
        if (cProd == null || cProd.isBlank()) {
            return Optional.empty();
        }
        Optional<ProdutoMatriz> porInterno = produtoMatrizRepository.findFirstByCodInternoProduto(cProd.trim());
        if (porInterno.isPresent()) {
            return porInterno;
        }
        List<ProdutoMatriz> lista = produtoMatrizRepository.findByCodProdFornecedor(cProd.trim());
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private static BigDecimal converterQuantidadeVenda(ItemNfeCfop ix, ProdutoMatriz pm) {
        BigDecimal q = ix.qCom() != null ? ix.qCom() : BigDecimal.ZERO;
        if (ix.uCom() != null
                && pm.getUnidadeInternaProduto() != null
                && ix.uCom().equalsIgnoreCase(pm.getUnidadeInternaProduto().trim())) {
            return q.setScale(6, RoundingMode.HALF_UP);
        }
        if (pm.getFatorConversao() != null && pm.getFatorConversao().compareTo(BigDecimal.ZERO) > 0) {
            return q.multiply(pm.getFatorConversao()).setScale(6, RoundingMode.HALF_UP);
        }
        return q.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Converte quantidade do C170 da entrada para unidade interna da matriz (mesma regra da venda a partir do XML).
     */
    private static BigDecimal converterQuantidadeEntradaC170(C170Linha c170, ProdutoMatriz pm) {
        BigDecimal q = c170.qtd() != null ? c170.qtd() : BigDecimal.ZERO;
        if (c170.unid() != null
                && pm.getUnidadeInternaProduto() != null
                && c170.unid().trim().equalsIgnoreCase(pm.getUnidadeInternaProduto().trim())) {
            return q.setScale(6, RoundingMode.HALF_UP);
        }
        if (pm.getFatorConversao() != null && pm.getFatorConversao().compareTo(BigDecimal.ZERO) > 0) {
            return q.multiply(pm.getFatorConversao()).setScale(6, RoundingMode.HALF_UP);
        }
        return q.setScale(6, RoundingMode.HALF_UP);
    }

    private static boolean conversaoEntradaAplicada(C170Linha c170, ProdutoMatriz pm) {
        if (c170.unid() == null || pm.getUnidadeInternaProduto() == null) {
            return false;
        }
        return !c170.unid().trim().equalsIgnoreCase(pm.getUnidadeInternaProduto().trim());
    }

    private Map<String, ArrayDeque<EntradaFifoSlot>> construirEstoqueFifo(
            List<ResumoNfLinhaDTO> linhasResumo,
            EfdIndice indice,
            ExecucaoFluxoPedido exec,
            List<String> avisos) {
        List<ResumoNfLinhaDTO> ordenadas = linhasResumo.stream()
                .sorted(Comparator.comparing(
                                ResumoNfLinhaDTO::getDataApresentacao,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ResumoNfLinhaDTO::getChave)
                        .thenComparingInt(ResumoNfLinhaDTO::getSeqItem))
                .toList();

        Map<String, ArrayDeque<EntradaFifoSlot>> map = new LinkedHashMap<>();
        for (ResumoNfLinhaDTO linha : ordenadas) {
            if (linha.getChave() == null || linha.getChave().length() != 44) {
                continue;
            }
            var notaEnt = indice.notaEntradaPorChave(linha.getChave());
            if (notaEnt.isEmpty()) {
                continue;
            }
            var c170Opt = notaEnt.get().findItem(linha.getSeqItem());
            if (c170Opt.isEmpty()) {
                continue;
            }
            C170Linha c170 = c170Opt.get();
            String codInt = c170.codItem();
            Optional<ProdutoMatriz> pmOpt = produtoMatrizRepository.findFirstByCodInternoProduto(codInt);
            if (pmOpt.isEmpty()) {
                String msg = "FIFO: sem produto na matriz para COD_ITEM=" + codInt + " (chave " + linha.getChave() + ")";
                avisos.add(msg);
                addLog(exec, "WARN", "ESTOQUE_FIFO", msg, linha.getChave());
                continue;
            }
            ProdutoMatriz pm = pmOpt.get();
            BigDecimal qtyInt = converterQuantidadeEntradaC170(c170, pm);
            if (qtyInt.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            map.computeIfAbsent(codInt, k -> new ArrayDeque<>())
                    .addLast(new EntradaFifoSlot(linha, c170, pm, qtyInt));
        }
        return map;
    }

    private static ResultadoConsumoFifo consumirFifo(
            BigDecimal qVendaInterna, String codInterno, Map<String, ArrayDeque<EntradaFifoSlot>> estoque) {
        BigDecimal need = qVendaInterna != null ? qVendaInterna : BigDecimal.ZERO;
        if (need.compareTo(BigDecimal.ZERO) < 0) {
            need = BigDecimal.ZERO;
        }

        ArrayDeque<EntradaFifoSlot> fila = estoque.get(codInterno);
        if (fila == null || fila.isEmpty() || need.compareTo(BigDecimal.ZERO) == 0) {
            boolean ok = need.compareTo(BigDecimal.ZERO) == 0;
            return new ResultadoConsumoFifo(
                    ok,
                    BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                    null,
                    new LinkedHashSet<>(),
                    List.of());
        }

        List<AuditoriaEntradaConsumida> linhas = new ArrayList<>();
        LinkedHashSet<String> chaves = new LinkedHashSet<>();
        NotaEntrada primeira = null;
        BigDecimal totalCompras = BigDecimal.ZERO;

        while (need.compareTo(BigDecimal.ZERO) > 0 && !fila.isEmpty()) {
            EntradaFifoSlot slot = fila.peekFirst();
            BigDecimal take = need.min(slot.remainingInterno);
            slot.remainingInterno = slot.remainingInterno.subtract(take);
            need = need.subtract(take);
            totalCompras = totalCompras.add(take);

            chaves.add(slot.linha.getChave());
            if (primeira == null) {
                primeira = NotaEntrada.builder().chaveNFeEntrada(slot.linha.getChave()).build();
            }

            BigDecimal qOrigTotal = slot.c170.qtd() != null ? slot.c170.qtd() : BigDecimal.ZERO;
            BigDecimal frac = slot.initialInterno.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : take.divide(slot.initialInterno, 10, RoundingMode.HALF_UP);
            BigDecimal qOrigPart = qOrigTotal.multiply(frac).setScale(6, RoundingMode.HALF_UP);

            boolean conv = conversaoEntradaAplicada(slot.c170, slot.pm);
            BigDecimal fatorAplicado =
                    conv && slot.pm.getFatorConversao() != null ? slot.pm.getFatorConversao() : null;

            AuditoriaEntradaConsumida lin = AuditoriaEntradaConsumida.builder()
                    .chaveNfeEntrada(slot.linha.getChave())
                    .seqItem(slot.linha.getSeqItem())
                    .codgItem(blankToNull(slot.linha.getCodgItem()))
                    .cnpjFornecedor(normalizarCnpj14(slot.linha.getCnpjFornecedor()))
                    .tributo(blankToNull(slot.linha.getTributo()))
                    .dataApresentacao(slot.linha.getDataApresentacao())
                    .quantidadeOriginalUnidadeFornecedor(qOrigPart)
                    .unidadeFornecedor(blankToNull(slot.c170.unid()))
                    .fatorConversaoAplicado(fatorAplicado)
                    .quantidadeConvertidaUnidadeInterna(take.setScale(6, RoundingMode.HALF_UP))
                    .conversaoAplicada(conv)
                    .quantidadeConsumida(take.setScale(6, RoundingMode.HALF_UP))
                    .build();
            linhas.add(lin);

            if (slot.remainingInterno.compareTo(BigDecimal.ZERO) <= 0) {
                fila.pollFirst();
            }
        }

        boolean suficiente = need.compareTo(BigDecimal.ZERO) <= 0;
        return new ResultadoConsumoFifo(
                suficiente,
                totalCompras.setScale(6, RoundingMode.HALF_UP),
                primeira,
                chaves,
                linhas);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String normalizarCnpj14(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String d = s.replaceAll("\\D", "");
        return d.length() == 14 ? d : null;
    }

    private record ResultadoConsumoFifo(
            boolean suficiente,
            BigDecimal totalComprasInterno,
            NotaEntrada primeiraEntrada,
            LinkedHashSet<String> chaves,
            List<AuditoriaEntradaConsumida> linhasAuditoria) {}

    private static final class EntradaFifoSlot {
        final ResumoNfLinhaDTO linha;
        final C170Linha c170;
        final ProdutoMatriz pm;
        final BigDecimal initialInterno;
        BigDecimal remainingInterno;

        EntradaFifoSlot(ResumoNfLinhaDTO linha, C170Linha c170, ProdutoMatriz pm, BigDecimal qtyInterno) {
            this.linha = linha;
            this.c170 = c170;
            this.pm = pm;
            this.initialInterno = qtyInterno;
            this.remainingInterno = qtyInterno;
        }
    }

    @Transactional(readOnly = true)
    public Page<ExecucaoFluxoPedidoResumoDTO> listarExecucoes(Pageable pageable) {
        Declarante decl = declaranteService.getEntidadeOuLanca();
        return execucaoRepository
                .findByDeclaranteIdOrderByDataHoraInicioDesc(decl.getId(), pageable)
                .map(e -> ExecucaoFluxoPedidoResumoDTO.builder()
                        .id(e.getId())
                        .anoReferencia(e.getAnoReferencia())
                        .mesReferencia(e.getMesReferencia())
                        .dataHoraInicio(e.getDataHoraInicio())
                        .dataHoraFim(e.getDataHoraFim())
                        .statusExecucao(e.getStatusExecucao())
                        .build());
    }

    @Transactional(readOnly = true)
    public RastreabilidadeFluxoDTO rastreabilidade(Long idExecucao) {
        ExecucaoFluxoPedido exec = execucaoRepository
                .findDetailedById(idExecucao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Execução não encontrada: " + idExecucao));
        Declarante decl = declaranteService.getEntidadeOuLanca();
        if (!exec.getDeclarante().getId().equals(decl.getId())) {
            throw new RecursoNaoEncontradoException("Execução não encontrada: " + idExecucao);
        }

        ExecucaoFluxoPedidoResumoDTO resumo = ExecucaoFluxoPedidoResumoDTO.builder()
                .id(exec.getId())
                .anoReferencia(exec.getAnoReferencia())
                .mesReferencia(exec.getMesReferencia())
                .dataHoraInicio(exec.getDataHoraInicio())
                .dataHoraFim(exec.getDataHoraFim())
                .statusExecucao(exec.getStatusExecucao())
                .build();

        List<AuditoriaProdutoVendidoDTO> prods = exec.getAuditoriasProduto().stream()
                .map(a -> {
                    List<AuditoriaEntradaConsumidaDTO> ent = a.getEntradasConsumidas().stream()
                            .map(e -> AuditoriaEntradaConsumidaDTO.builder()
                                    .chaveNfeEntrada(e.getChaveNfeEntrada())
                                    .seqItem(e.getSeqItem())
                                    .codgItem(e.getCodgItem())
                                    .cnpjFornecedor(e.getCnpjFornecedor())
                                    .tributo(e.getTributo())
                                    .dataApresentacao(e.getDataApresentacao())
                                    .quantidadeOriginalUnidadeFornecedor(
                                            e.getQuantidadeOriginalUnidadeFornecedor())
                                    .unidadeFornecedor(e.getUnidadeFornecedor())
                                    .fatorConversaoAplicado(e.getFatorConversaoAplicado())
                                    .quantidadeConvertidaUnidadeInterna(
                                            e.getQuantidadeConvertidaUnidadeInterna())
                                    .quantidadeConsumida(e.getQuantidadeConsumida())
                                    .conversaoAplicada(e.isConversaoAplicada())
                                    .build())
                            .collect(Collectors.toList());
                    return AuditoriaProdutoVendidoDTO.builder()
                            .codInternoProduto(a.getCodInternoProduto())
                            .chaveNfeSaida(a.getChaveNfeSaida())
                            .numItemNfe(a.getNumItemNfe())
                            .cfopItem(a.getCfopItem())
                            .quantidadeVendidaUnidadeInterna(a.getQuantidadeVendidaUnidadeInterna())
                            .unidadeInterna(a.getUnidadeInterna())
                            .quantidadeTotalComprasConvertida(a.getQuantidadeTotalComprasConvertida())
                            .suficiente(a.isSuficiente())
                            .entradasConsumidas(ent)
                            .build();
                })
                .collect(Collectors.toList());

        List<Map<String, String>> logs = exec.getLogs().stream()
                .sorted(Comparator.comparing(LogExecucaoFluxo::getTs))
                .map(l -> Map.of(
                        "nivel", l.getNivel(),
                        "etapa", l.getEtapa(),
                        "mensagem", l.getMensagem(),
                        "ts", l.getTs().toString()))
                .collect(Collectors.toList());

        return RastreabilidadeFluxoDTO.builder()
                .execucao(resumo)
                .produtosVendidos(prods)
                .logs(logs)
                .build();
    }

    private static Path exigirDir(String caminho, String prop) {
        if (!StringUtils.hasText(caminho)) {
            throw new IllegalArgumentException("Configure " + prop);
        }
        Path p = Paths.get(caminho);
        if (!Files.isDirectory(p)) {
            throw new RecursoNaoEncontradoException("Diretório não encontrado: " + p.toAbsolutePath());
        }
        return p;
    }

    private static Path resolverPrimeiroXlsx(Path dirResumo) throws IOException {
        try (Stream<Path> stream = Files.list(dirResumo)) {
            List<Path> xs = stream
                    .filter(Files::isRegularFile)
                    .filter(x -> x.getFileName().toString().toLowerCase().endsWith(".xlsx"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
            if (xs.isEmpty()) {
                throw new RecursoNaoEncontradoException("Nenhum .xlsx em " + dirResumo.toAbsolutePath());
            }
            return xs.get(0);
        }
    }
}
