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
import br.com.empresa.ressarcimento.pedidos.NotaEntradaRepository;
import br.com.empresa.ressarcimento.pedidos.NotaSaidaRepository;
import br.com.empresa.ressarcimento.pedidos.domain.NotaEntrada;
import br.com.empresa.ressarcimento.pedidos.domain.NotaSaida;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditItemNfeSaida;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditNfeSaida;
import br.com.empresa.ressarcimento.pedidos.fluxo.audit.FluxoBAuditStagingService;
import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import br.com.empresa.ressarcimento.produtos.ProdutoMatrizRepository;
import br.com.empresa.ressarcimento.produtos.automatizado.ItemNfeCfop;
import br.com.empresa.ressarcimento.produtos.automatizado.LeitorNfeUcom;
import br.com.empresa.ressarcimento.produtos.automatizado.LeitorResumoNf;
import br.com.empresa.ressarcimento.produtos.automatizado.NfeIdeCampos;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.C170Linha;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.EfdIndice;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.ParserEfdService;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoLifecycle;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoRepository;
import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
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
import java.util.Objects;
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

    private static final String STG_SAIDA_SEM_XML = "SEM_XML";
    private static final String STG_SAIDA_SEM_ITENS_CFOP = "SEM_ITENS_CFOP";
    private static final String STG_SAIDA_OK = "OK";
    private static final String STG_SAIDA_ERRO_LEITURA = "ERRO_LEITURA_XML";

    private final RessarcimentoProperties properties;
    private final ParserEfdService parserEfdService;
    private final LeitorNfeUcom leitorNfeUcom;
    private final LeitorResumoNf leitorResumoNf;
    private final ProdutoMatrizRepository produtoMatrizRepository;
    private final DeclaranteService declaranteService;
    private final GeradorXmlPedidos geradorXmlPedidos;
    private final ArquivoPedidoRepository arquivoPedidoRepository;
    private final FluxoBAuditStagingService fluxoBAuditStagingService;
    private final ProcessamentoRessarcimentoRepository processamentoRessarcimentoRepository;
    private final ProcessamentoRessarcimentoLifecycle processamentoRessarcimentoLifecycle;
    private final NotaSaidaRepository notaSaidaRepository;
    private final NotaEntradaRepository notaEntradaRepository;

    @Transactional
    public GerarPedidoAutomaticoResponse gerarAutomatico(int ano, int mes) throws JAXBException, IOException {
        return gerarAutomatico(ano, mes, null);
    }

    @Transactional
    public GerarPedidoAutomaticoResponse gerarAutomatico(int ano, int mes, Long processamentoRessarcimentoId)
            throws JAXBException, IOException {
        Declarante decl = declaranteService.getEntidadeOuLanca();
        String anoStr = String.format("%04d", ano);
        String mesStr = mes >= 1 && mes <= 9 ? "0" + mes : String.valueOf(mes);

        Path dirEfd = exigirDir(properties.getEfdsDir(), "ressarcimento.efds-dir");
        Path dirNfeSaida = exigirDir(properties.getNfesSaidaDir(), "ressarcimento.nfes-saida-dir");
        Path dirNfeEntrada = exigirDir(properties.getNfesDir(), "ressarcimento.nfes-dir");
        Path dirResumo = exigirDir(properties.getResumoNotasDir(), "ressarcimento.resumo-notas-dir");
        Path arquivoResumo = resolverPrimeiroXlsx(dirResumo);

        fluxoBAuditStagingService.limparStaging();

        ProcessamentoRessarcimento procRef;
        if (processamentoRessarcimentoId != null) {
            procRef = processamentoRessarcimentoRepository
                    .findById(processamentoRessarcimentoId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Processamento de ressarcimento não encontrado: " + processamentoRessarcimentoId));
        } else {
            procRef = processamentoRessarcimentoLifecycle.iniciarEmAndamento(ano, mes);
        }
        Long pidRastreio = procRef.getId();
        procRef.setStatusExecucao(STATUS_EM_ANDAMENTO);
        procRef.setArquivoEfdUtilizado(dirEfd.toString());
        procRef.setPastaNfesSaida(dirNfeSaida.toString());
        procRef.setPastaNfesEntrada(dirNfeEntrada.toString());
        procRef.setArquivoResumonf(arquivoResumo.toString());
        procRef = processamentoRessarcimentoRepository.save(procRef);
        MDC.put("idProcessamento", String.valueOf(pidRastreio));
        log.info("Início Fluxo B idProcessamento={} período={}-{}", pidRastreio, anoStr, mesStr);

        List<String> avisos = new ArrayList<>();

        try {
            EfdIndice indice = parserEfdService.carregarDiretorio(dirEfd);
            List<String> chavesSaida = new ArrayList<>(indice.chavesSaidaNoMes(ano, mes));
            chavesSaida.sort(Comparator.comparing(
                                    (String k) -> indice.dataDocumentoSaida(k).orElse(null),
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(k -> k));
            if (chavesSaida.isEmpty()) {
                addLog(procRef, "WARN", "LEITURA_EFD", "Nenhuma NF-e de saída modelo 55 no período na EFD.", null);
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

            try {
                fluxoBAuditStagingService.persistirEntradasDoResumo(
                        linhasResumo, dirNfeEntrada, leitorNfeUcom, procRef, indice);
            } catch (Exception e) {
                throw new IOException("Falha ao gravar staging de auditoria (resumo NF entrada): " + e.getMessage(), e);
            }

            Map<String, ArrayDeque<EntradaFifoSlot>> estoqueFifo =
                    construirEstoqueFifo(linhasResumo, indice, procRef, avisos);

            List<NotaSaida> notasMontadas = new ArrayList<>();

            for (String chaveSaida : chavesSaida) {
                Optional<Path> xmlOpt = leitorNfeUcom.localizarArquivoXml(dirNfeSaida, chaveSaida);
                if (xmlOpt.isEmpty()) {
                    fluxoBAuditStagingService.salvarNfeSaida(FluxoBAuditNfeSaida.builder()
                            .chaveNFe(chaveSaida)
                            .statusProcessamento(STG_SAIDA_SEM_XML)
                            .processamentoRessarcimento(procRef)
                            .build());
                    String msg = "XML de NF-e de saída não encontrado para chave " + chaveSaida;
                    avisos.add(msg);
                    addLog(procRef, "WARN", "LEITURA_XML_SAIDA", msg, chaveSaida);
                    continue;
                }

                FluxoBAuditNfeSaida auditSaida = FluxoBAuditNfeSaida.builder()
                        .chaveNFe(chaveSaida)
                        .statusProcessamento(STG_SAIDA_ERRO_LEITURA)
                        .processamentoRessarcimento(procRef)
                        .build();
                List<ItemNfeCfop> itensXml;
                try {
                    Optional<NfeIdeCampos> ideOpt = leitorNfeUcom.lerIdeCampos(xmlOpt.get());
                    if (ideOpt.isPresent()) {
                        NfeIdeCampos ide = ideOpt.get();
                        auditSaida.setDhSaiEnt(truncarIde(ide.dhSaiEnt(), 35));
                        auditSaida.setDhEmi(truncarIde(ide.dhEmi(), 35));
                        auditSaida.setDEmi(truncarIde(ide.dEmi(), 12));
                    }
                    indice.dataDocumentoSaida(chaveSaida).ifPresent(auditSaida::setDataDocEfd);
                    itensXml = leitorNfeUcom.listarItensComCfops(xmlOpt.get(), CFOPS_FLUXO_B);
                } catch (Exception e) {
                    fluxoBAuditStagingService.salvarNfeSaida(auditSaida);
                    avisos.add("Falha ao ler XML saída " + chaveSaida + ": " + e.getMessage());
                    addLog(procRef, "ERROR", "LEITURA_XML_SAIDA", e.getMessage(), chaveSaida);
                    continue;
                }
                if (itensXml.isEmpty()) {
                    auditSaida.setStatusProcessamento(STG_SAIDA_SEM_ITENS_CFOP);
                    fluxoBAuditStagingService.salvarNfeSaida(auditSaida);
                    continue;
                }

                String cfopsAgg = itensXml.stream()
                        .map(ItemNfeCfop::cfop)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(","));
                if (cfopsAgg.length() > 200) {
                    cfopsAgg = cfopsAgg.substring(0, 197) + "...";
                }
                auditSaida.setCfopsItensElegiveis(cfopsAgg);
                auditSaida.setStatusProcessamento(STG_SAIDA_OK);
                auditSaida = fluxoBAuditStagingService.salvarNfeSaida(auditSaida);

                for (ItemNfeCfop ix : itensXml) {
                    Optional<ProdutoMatriz> pmStg = resolverProdutoPorCProdSaida(ix.cProd());
                    fluxoBAuditStagingService.salvarItemNfeSaida(FluxoBAuditItemNfeSaida.builder()
                            .auditNfeSaida(auditSaida)
                            .numItemNFe(ix.nItem())
                            .cProd(ix.cProd())
                            .cfop(ix.cfop())
                            .qCom(ix.qCom() != null ? ix.qCom() : BigDecimal.ZERO)
                            .produtoMatriz(pmStg.orElse(null))
                            .codInternoResolvido(pmStg.map(ProdutoMatriz::getCodInternoProduto).orElse(null))
                            .build());
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
                        addLog(procRef, "WARN", "MAPEAMENTO_PRODUTO", msg, null);
                        continue;
                    }
                    ProdutoMatriz pm = pmOpt.get();
                    String codInt = pm.getCodInternoProduto();

                    BigDecimal qVenda = converterQuantidadeVenda(ix, pm);
                    ResultadoConsumoFifo consumo = consumirFifo(qVenda, codInt, estoqueFifo, procRef);

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
                            .processamentoRessarcimento(procRef)
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
                    procRef.getAuditoriasProdutoFluxoB().add(aud);
                }

                if (!ns.getItens().isEmpty()) {
                    notasMontadas.add(ns);
                }
            }

            if (notasMontadas.isEmpty()) {
                procRef.setStatusExecucao(STATUS_ERRO);
                procRef.setDataHoraFim(LocalDateTime.now());
                addLog(procRef, "ERROR", "GERACAO_XML", "Nenhuma operação com itens CFOP 6102/6108 elegível.", null);
                processamentoRessarcimentoRepository.save(procRef);
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
                    .processamentoRessarcimento(procRef)
                    .build();
            arq = arquivoPedidoRepository.save(arq);

            persistirNotasSeRastreio(pidRastreio, decl, notasMontadas);

            procRef.setDataHoraFim(LocalDateTime.now());
            procRef.setStatusExecucao(avisos.isEmpty() ? STATUS_CONCLUIDO : STATUS_CONCLUIDO_AVISOS);
            processamentoRessarcimentoRepository.save(procRef);

            log.info("Fim Fluxo B idProcessamento={} status={}", pidRastreio, procRef.getStatusExecucao());
            return GerarPedidoAutomaticoResponse.builder()
                    .processamentoRessarcimentoId(pidRastreio)
                    .arquivoPedidoId(arq.getId())
                    .status(procRef.getStatusExecucao())
                    .avisos(avisos)
                    .build();
        } catch (Exception e) {
            procRef.setDataHoraFim(LocalDateTime.now());
            procRef.setStatusExecucao(STATUS_ERRO);
            addLog(
                    procRef,
                    "ERROR",
                    "GERAL",
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    null);
            processamentoRessarcimentoRepository.save(procRef);
            throw e;
        } finally {
            MDC.remove("idProcessamento");
        }
    }

    private void addLog(ProcessamentoRessarcimento proc, String nivel, String etapa, String mensagem, String detalhes) {
        LogExecucaoFluxo lg = LogExecucaoFluxo.builder()
                .processamentoRessarcimento(proc)
                .nivel(nivel)
                .etapa(etapa)
                .mensagem(mensagem != null && mensagem.length() > 1000 ? mensagem.substring(0, 997) + "..." : mensagem)
                .detalhes(detalhes)
                .ts(LocalDateTime.now())
                .build();
        proc.getLogsFluxoPedido().add(lg);
    }

    /**
     * Garante rastreio em {@code nota_saida.processamento_ressarcimento_id}: insere notas novas ou atualiza o FK
     * quando a chave já existe (ex.: importação manual anterior sem processamento).
     * Quando a saída já existe, também alinha {@code nota_entrada.processamento_ressarcimento_id} às chaves dos itens
     * montados neste Fluxo B (evita saída no processamento atual e entradas presas ao processamento anterior).
     */
    private void persistirNotasSeRastreio(
            Long processamentoRessarcimentoId, Declarante decl, List<NotaSaida> notasMontadas) {
        if (notasMontadas == null || notasMontadas.isEmpty()) {
            return;
        }
        ProcessamentoRessarcimento procRef =
                processamentoRessarcimentoRepository.getReferenceById(processamentoRessarcimentoId);
        Long declId = decl.getId();
        for (NotaSaida ns : notasMontadas) {
            Optional<NotaSaida> existenteOpt = notaSaidaRepository.findByChaveNFe(ns.getChaveNFe());
            if (existenteOpt.isPresent()) {
                NotaSaida existente = existenteOpt.get();
                if (!declId.equals(existente.getDeclarante().getId())) {
                    log.warn(
                            "Nota de saída chave={} pertence a outro declarante; não vincula ao processamento {}.",
                            ns.getChaveNFe(),
                            processamentoRessarcimentoId);
                    continue;
                }
                ProcessamentoRessarcimento procAtual = existente.getProcessamentoRessarcimento();
                Long procAtualId = procAtual == null ? null : procAtual.getId();
                if (!processamentoRessarcimentoId.equals(procAtualId)) {
                    existente.setProcessamentoRessarcimento(procRef);
                    notaSaidaRepository.save(existente);
                }
                alinharProcessamentoEntradasReferenciadasPelosItens(
                        procRef, processamentoRessarcimentoId, ns.getItens());
                continue;
            }
            NotaSaida persisted = NotaSaida.builder()
                    .declarante(decl)
                    .chaveNFe(ns.getChaveNFe())
                    .anoPeriodoReferencia(ns.getAnoPeriodoReferencia())
                    .mesPeriodoReferencia(ns.getMesPeriodoReferencia())
                    .processamentoRessarcimento(procRef)
                    .itens(new ArrayList<>())
                    .build();
            for (ItemNotaSaida it : ns.getItens()) {
                NotaEntrada ne = null;
                if (it.getNotaEntrada() != null && StringUtils.hasText(it.getNotaEntrada().getChaveNFeEntrada())) {
                    ne = obterOuCriarNotaEntradaComProcessamento(
                            it.getNotaEntrada().getChaveNFeEntrada(), procRef, processamentoRessarcimentoId);
                }
                ItemNotaSaida ni = ItemNotaSaida.builder()
                        .notaSaida(persisted)
                        .produtoMatriz(it.getProdutoMatriz())
                        .codInternoProduto(it.getCodInternoProduto())
                        .numItemNFe(it.getNumItemNFe())
                        .notaEntrada(ne)
                        .build();
                ni.getChavesNfeEntradaConsumidas().addAll(it.getChavesNfeEntradaConsumidas());
                persisted.getItens().add(ni);
            }
            alinharProcessamentoEntradasReferenciadasPelosItens(
                    procRef, processamentoRessarcimentoId, ns.getItens());
            notaSaidaRepository.save(persisted);
        }
    }

    private NotaEntrada obterOuCriarNotaEntradaComProcessamento(
            String chaveNFe44,
            ProcessamentoRessarcimento procRef,
            long processamentoRessarcimentoId) {
        String chaveEnt = chaveNFe44.trim();
        Optional<NotaEntrada> neOpt = notaEntradaRepository.findByChaveNFeEntrada(chaveEnt);
        if (neOpt.isPresent()) {
            NotaEntrada ne = neOpt.get();
            Long procNeId =
                    ne.getProcessamentoRessarcimento() == null ? null : ne.getProcessamentoRessarcimento().getId();
            if (!Objects.equals(processamentoRessarcimentoId, procNeId)) {
                ne.setProcessamentoRessarcimento(procRef);
                return notaEntradaRepository.save(ne);
            }
            return ne;
        }
        return notaEntradaRepository.save(NotaEntrada.builder()
                .chaveNFeEntrada(chaveEnt)
                .processamentoRessarcimento(procRef)
                .build());
    }

    /**
     * Atualiza ou cria {@link NotaEntrada} para cada chave referenciada nos itens em memória (principal + FIFO).
     */
    private void alinharProcessamentoEntradasReferenciadasPelosItens(
            ProcessamentoRessarcimento procRef,
            long processamentoRessarcimentoId,
            List<ItemNotaSaida> itens) {
        if (itens == null || itens.isEmpty()) {
            return;
        }
        for (ItemNotaSaida it : itens) {
            if (it.getNotaEntrada() != null && StringUtils.hasText(it.getNotaEntrada().getChaveNFeEntrada())) {
                obterOuCriarNotaEntradaComProcessamento(
                        it.getNotaEntrada().getChaveNFeEntrada(), procRef, processamentoRessarcimentoId);
            }
            if (it.getChavesNfeEntradaConsumidas() != null) {
                for (String chave : it.getChavesNfeEntradaConsumidas()) {
                    if (!StringUtils.hasText(chave)) {
                        continue;
                    }
                    String t = chave.trim();
                    if (t.length() == 44) {
                        obterOuCriarNotaEntradaComProcessamento(t, procRef, processamentoRessarcimentoId);
                    }
                }
            }
        }
    }

    /**
     * Mapeia item da NF-e de saída: {@code cProd} do XML deve coincidir com {@code cod_interno_produto} na matriz
     * (não usar {@code cod_prod_fornecedor}). Se houver várias linhas com o mesmo código interno, retorna a primeira
     * retornada pelo repositório.
     */
    private static String truncarIde(String valor, int maxLen) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String t = valor.trim();
        return t.length() <= maxLen ? t : t.substring(0, maxLen);
    }

    private Optional<ProdutoMatriz> resolverProdutoPorCProdSaida(String cProd) {
        if (cProd == null || cProd.isBlank()) {
            return Optional.empty();
        }
        return produtoMatrizRepository.findFirstByCodInternoProduto(cProd.trim());
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
            ProcessamentoRessarcimento proc,
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
                addLog(proc, "WARN", "ESTOQUE_FIFO", msg, linha.getChave());
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
            BigDecimal qVendaInterna,
            String codInterno,
            Map<String, ArrayDeque<EntradaFifoSlot>> estoque,
            ProcessamentoRessarcimento processamentoRessarcimento) {
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
                    .processamentoRessarcimento(processamentoRessarcimento)
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
        return processamentoRessarcimentoRepository
                .findByDeclaranteIdAndArquivoEfdUtilizadoIsNotNullOrderByDataHoraInicioDesc(decl.getId(), pageable)
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
    public RastreabilidadeFluxoDTO rastreabilidade(Long idProcessamento) {
        ProcessamentoRessarcimento proc = processamentoRessarcimentoRepository
                .findDetailedFluxoBById(idProcessamento)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Processamento de Fluxo B não encontrado: " + idProcessamento));
        Declarante decl = declaranteService.getEntidadeOuLanca();
        if (!proc.getDeclarante().getId().equals(decl.getId())) {
            throw new RecursoNaoEncontradoException(
                    "Processamento de Fluxo B não encontrado: " + idProcessamento);
        }

        ExecucaoFluxoPedidoResumoDTO resumo = ExecucaoFluxoPedidoResumoDTO.builder()
                .id(proc.getId())
                .anoReferencia(proc.getAnoReferencia())
                .mesReferencia(proc.getMesReferencia())
                .dataHoraInicio(proc.getDataHoraInicio())
                .dataHoraFim(proc.getDataHoraFim())
                .statusExecucao(proc.getStatusExecucao())
                .build();

        List<AuditoriaProdutoVendidoDTO> prods = proc.getAuditoriasProdutoFluxoB().stream()
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

        List<Map<String, String>> logs = proc.getLogsFluxoPedido().stream()
                .sorted(Comparator.comparing(LogExecucaoFluxo::getTs))
                .map(l -> Map.of(
                        "nivel", l.getNivel(),
                        "etapa", l.getEtapa(),
                        "mensagem", l.getMensagem(),
                        "ts", l.getTs().toString()))
                .collect(Collectors.toList());

        return RastreabilidadeFluxoDTO.builder()
                .processamento(resumo)
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
