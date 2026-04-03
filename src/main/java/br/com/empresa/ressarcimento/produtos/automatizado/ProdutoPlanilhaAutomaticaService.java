package br.com.empresa.ressarcimento.produtos.automatizado;

import br.com.empresa.ressarcimento.config.RessarcimentoProperties;
import br.com.empresa.ressarcimento.planilhas.dto.ProdutoPlanilhaDTO;
import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import br.com.empresa.ressarcimento.produtos.api.GerarPlanilhaAutomaticaRequest;
import br.com.empresa.ressarcimento.produtos.api.LogGeracaoPlanilhaDTO;
import br.com.empresa.ressarcimento.produtos.api.ResultadoGeracaoPlanilhaAutomatica;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoLifecycle;
import br.com.empresa.ressarcimento.processamento.ProcessamentoRessarcimentoRepository;
import br.com.empresa.ressarcimento.produtos.automatizado.domain.LogGeracaoPlanilha;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.C170Linha;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.EfdIndice;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.InfoItemSped;
import br.com.empresa.ressarcimento.produtos.automatizado.efd.ParserEfdService;
import br.com.empresa.ressarcimento.shared.exception.RecursoNaoEncontradoException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProdutoPlanilhaAutomaticaService {

    private static final BigDecimal FATOR_PADRAO = new BigDecimal("1.000000");

    private final RessarcimentoProperties properties;
    private final ParserEfdService parserEfdService;
    private final LeitorResumoNf leitorResumoNf;
    private final LeitorNfeUcom leitorNfeUcom;
    private final EscritorPlanilhaProdutosExcel escritorPlanilhaProdutosExcel;
    private final LogGeracaoPlanilhaRepository logRepository;
    private final ProcessamentoRessarcimentoRepository processamentoRessarcimentoRepository;
    private final ProcessamentoRessarcimentoLifecycle processamentoRessarcimentoLifecycle;

    @Transactional
    public ResultadoGeracaoPlanilhaAutomatica gerarPlanilhaAutomatica(GerarPlanilhaAutomaticaRequest req)
            throws IOException {
        return gerarPlanilhaAutomatica(req, null);
    }

    @Transactional
    public ResultadoGeracaoPlanilhaAutomatica gerarPlanilhaAutomatica(
            GerarPlanilhaAutomaticaRequest req, Long processamentoRessarcimentoId) throws IOException {
        GerarPlanilhaAutomaticaRequest r = req != null ? req : new GerarPlanilhaAutomaticaRequest();
        Path dirResumo = exigirDiretorio(properties.getResumoNotasDir(), "ressarcimento.resumo-notas-dir");
        Path dirEfd = exigirDiretorio(properties.getEfdsDir(), "ressarcimento.efds-dir");
        Path dirNfes = exigirDiretorio(properties.getNfesDir(), "ressarcimento.nfes-dir");
        Path arquivoResumo = resolverArquivoResumo(dirResumo, r.getAnoReferencia(), r.getMesReferencia(), r.getNomeArquivoResumo());
        return gerarPlanilhaAutomaticaDeFontes(arquivoResumo, dirEfd, dirNfes, r, processamentoRessarcimentoId);
    }

    /**
     * Fluxo A a partir de arquivos já materializados no disco (ex.: upload multipart descompactado em diretório temporário).
     */
    @Transactional
    public ResultadoGeracaoPlanilhaAutomatica gerarPlanilhaAutomaticaUpload(
            Path arquivoResumo, Path dirEfd, Path dirNfes, GerarPlanilhaAutomaticaRequest req) throws IOException {
        return gerarPlanilhaAutomaticaUpload(arquivoResumo, dirEfd, dirNfes, req, null);
    }

    @Transactional
    public ResultadoGeracaoPlanilhaAutomatica gerarPlanilhaAutomaticaUpload(
            Path arquivoResumo,
            Path dirEfd,
            Path dirNfes,
            GerarPlanilhaAutomaticaRequest req,
            Long processamentoRessarcimentoId)
            throws IOException {
        GerarPlanilhaAutomaticaRequest r = req != null ? req : new GerarPlanilhaAutomaticaRequest();
        return gerarPlanilhaAutomaticaDeFontes(arquivoResumo, dirEfd, dirNfes, r, processamentoRessarcimentoId);
    }

    /**
     * Único ponto que apaga {@code log_geracao_planilha}; importação manual de produtos não deve limpar estes logs.
     */
    private ResultadoGeracaoPlanilhaAutomatica gerarPlanilhaAutomaticaDeFontes(
            Path arquivoResumo,
            Path dirEfd,
            Path dirNfes,
            GerarPlanilhaAutomaticaRequest r,
            Long processamentoRessarcimentoId)
            throws IOException {
        logRepository.deleteAllInBatch();
        List<String> nomesArquivosEfd = listarNomesEfd(dirEfd);
        EfdIndice indice = parserEfdService.carregarDiretorio(dirEfd);

        List<ResumoNfLinhaDTO> linhasResumo;
        try (InputStream in = Files.newInputStream(arquivoResumo)) {
            linhasResumo = leitorResumoNf.lerExcel(in);
        }

        linhasResumo = filtrarPorPeriodo(linhasResumo, r.getAnoReferencia(), r.getMesReferencia());

        LocalDate hoje = LocalDate.now();
        int anoEfetivo = r.getAnoReferencia() != null ? r.getAnoReferencia() : hoje.getYear();
        int mesEfetivo = r.getMesReferencia() != null ? r.getMesReferencia() : hoje.getMonthValue();
        long pidResolvido = processamentoRessarcimentoId != null
                ? processamentoRessarcimentoId
                : processamentoRessarcimentoLifecycle.iniciarEmAndamento(anoEfetivo, mesEfetivo).getId();

        List<LogGeracaoPlanilha> logs = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();

        Map<String, ProdutoPlanilhaDTO> dedup = new LinkedHashMap<>();
        int rejeitadas = 0;
        int montadasParaDedup = 0;

        for (ResumoNfLinhaDTO linha : linhasResumo) {
            String chave = linha.getChave();
            int seq = linha.getSeqItem();
            String origemLinha = arquivoResumo.getFileName() + ":linha " + linha.getNumeroLinhaPlanilha();

            if (chave.length() != 44 || !chave.chars().allMatch(Character::isDigit)) {
                rejeitadas++;
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.DADO_INVALIDO,
                        chave.length() == 44 ? chave : null,
                        seq > 0 ? seq : null,
                        origemLinha,
                        "CHAVE deve ter 44 dígitos numéricos.",
                        agora));
                continue;
            }
            if (seq <= 0) {
                rejeitadas++;
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.DADO_INVALIDO,
                        chave,
                        null,
                        origemLinha,
                        "SEQ. ITEM inválido.",
                        agora));
                continue;
            }
            String cnpj = linha.getCnpjFornecedor();
            if (cnpj.length() != 14 || !cnpj.chars().allMatch(Character::isDigit)) {
                rejeitadas++;
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.DADO_INVALIDO,
                        chave,
                        seq,
                        origemLinha,
                        "CNPJ FORNECEDOR deve ter 14 dígitos numéricos.",
                        agora));
                continue;
            }
            String codg = linha.getCodgItem();
            if (!StringUtils.hasText(codg)) {
                rejeitadas++;
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.DADO_INVALIDO,
                        chave,
                        seq,
                        origemLinha,
                        "CODG. ITEM não pode ser vazio.",
                        agora));
                continue;
            }

            var notaOpt = indice.notaEntradaPorChave(chave);
            if (notaOpt.isEmpty()) {
                rejeitadas++;
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.NOTA_NAO_ENCONTRADA_EFD,
                        chave,
                        seq,
                        String.join(", ", nomesArquivosEfd),
                        "NF-e não encontrada em nenhum arquivo EFD (C100 CHV_NFE).",
                        agora));
                continue;
            }
            var c170Opt = notaOpt.get().findItem(seq);
            if (c170Opt.isEmpty()) {
                rejeitadas++;
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.ITEM_NAO_ENCONTRADO_NO_EFD,
                        chave,
                        seq,
                        String.join(", ", nomesArquivosEfd),
                        "Item não encontrado no registro C170 para NUM_ITEM = " + seq + ".",
                        agora));
                continue;
            }
            C170Linha c170 = c170Opt.get();
            InfoItemSped info0200 =
                    indice.infoItem(c170.codItem()).orElse(null);
            if (info0200 == null || !StringUtils.hasText(info0200.getDescrItem())) {
                rejeitadas++;
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.DADO_INVALIDO,
                        chave,
                        seq,
                        origemLinha,
                        "Registro 0200 ausente ou sem descrição para COD_ITEM " + c170.codItem() + ".",
                        agora));
                continue;
            }

            BigDecimal fator;
            if (info0200.getFatorConversao0220() != null) {
                fator = info0200.getFatorConversao0220().setScale(6, RoundingMode.HALF_UP);
            } else {
                fator = FATOR_PADRAO;
            }

            String unidadeInternaEfd;
            if (StringUtils.hasText(info0200.getUnidInv())) {
                unidadeInternaEfd = info0200.getUnidInv().trim();
            } else {
                unidadeInternaEfd = c170.unid();
            }
            if (StringUtils.hasText(unidadeInternaEfd) && !indice.existeUnidade0190(unidadeInternaEfd)) {
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.DADO_INVALIDO,
                        chave,
                        seq,
                        origemLinha,
                        "UNID_INV '" + unidadeInternaEfd + "' não encontrada no registro 0190 da EFD.",
                        agora));
            }

            String unidadeFornecedor = "";
            Path xmlNfe = null;
            try {
                var xmlOpt = leitorNfeUcom.localizarArquivoXml(dirNfes, chave);
                if (xmlOpt.isEmpty()) {
                    logs.add(log(pidResolvido,
                            TipoLogGeracaoPlanilha.XML_NFE_NAO_ENCONTRADO,
                            chave,
                            seq,
                            dirNfes.toString(),
                            "XML da NF-e de entrada não encontrado na pasta configurada. "
                                    + "unidade_fornecedor ficará em branco.",
                            agora));
                } else {
                    xmlNfe = xmlOpt.get();
                    try {
                        var uOpt = leitorNfeUcom.extrairUcom(xmlNfe, seq, codg);
                        if (uOpt.isEmpty()) {
                            logs.add(log(pidResolvido,
                                    TipoLogGeracaoPlanilha.DADO_INVALIDO,
                                    chave,
                                    seq,
                                    xmlNfe.getFileName().toString(),
                                    "uCom não localizado no XML para nItem=" + seq
                                            + " (e CODG. ITEM se informado). unidade_fornecedor ficará em branco.",
                                    agora));
                        } else {
                            String rawU = uOpt.get().trim();
                            if (!uComDaNFeUtilizavel(rawU)) {
                                logs.add(log(pidResolvido,
                                        TipoLogGeracaoPlanilha.DADO_INVALIDO,
                                        chave,
                                        seq,
                                        xmlNfe.getFileName().toString(),
                                        "uCom fora do limite 1–6 caracteres (manual NF-e, grupo prod): " + rawU
                                                + ". unidade_fornecedor ficará em branco.",
                                        agora));
                            } else {
                                unidadeFornecedor = rawU;
                            }
                        }
                    } catch (Exception e) {
                        logs.add(log(pidResolvido,
                                TipoLogGeracaoPlanilha.DADO_INVALIDO,
                                chave,
                                seq,
                                xmlNfe.getFileName().toString(),
                                "Falha ao ler XML: " + e.getMessage()
                                        + ". unidade_fornecedor ficará em branco.",
                                agora));
                    }
                }
            } catch (IOException e) {
                logs.add(log(pidResolvido,
                        TipoLogGeracaoPlanilha.XML_NFE_NAO_ENCONTRADO,
                        chave,
                        seq,
                        dirNfes.toString(),
                        "Erro ao localizar XML: " + e.getMessage()
                                + ". unidade_fornecedor ficará em branco.",
                        agora));
            }

            String descr = info0200.getDescrItem();
            if (descr.length() > 100) {
                descr = descr.substring(0, 100);
            }

            ProdutoPlanilhaDTO dto = ProdutoPlanilhaDTO.builder()
                    .numeroLinha(linha.getNumeroLinhaPlanilha())
                    .codInternoProduto(c170.codItem())
                    .descricaoProduto(descr)
                    .unidadeInternaProduto(unidadeInternaEfd)
                    .fatorConversao(fator)
                    .cnpjFornecedor(cnpj)
                    .codProdFornecedor(codg.trim())
                    .unidadeProdutoFornecedor(unidadeFornecedor)
                    .build();

            String chaveDedup = String.join(
                    "|",
                    dto.getCodInternoProduto(),
                    dto.getCnpjFornecedor(),
                    dto.getCodProdFornecedor(),
                    dto.getUnidadeInternaProduto(),
                    dto.getUnidadeProdutoFornecedor());
            dedup.putIfAbsent(chaveDedup, dto);
            montadasParaDedup++;
        }

        if (!logs.isEmpty()) {
            logRepository.saveAll(logs);
        }

        List<ProdutoPlanilhaDTO> saida = new ArrayList<>(dedup.values());
        int produtosUnicos = saida.size();
        int colapsadasDedup = Math.max(0, montadasParaDedup - produtosUnicos);

        byte[] xlsx = escritorPlanilhaProdutosExcel.escrever(saida);
        return ResultadoGeracaoPlanilhaAutomatica.builder()
                .planilhaXlsx(xlsx)
                .totalProdutosGerados(produtosUnicos)
                .totalLinhasResumo(linhasResumo.size())
                .totalLogs(logs.size())
                .totalLinhasIgnoradasTributo(0)
                .totalLinhasRejeitadas(rejeitadas)
                .totalLinhasMontadasParaDedup(montadasParaDedup)
                .totalLinhasColapsadasNaDedup(colapsadasDedup)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<LogGeracaoPlanilhaDTO> listarLogs(Pageable pageable) {
        return logRepository
                .findAllByOrderByDataProcessamentoDesc(pageable)
                .map(ProdutoPlanilhaAutomaticaService::toLogDto);
    }

    private static LogGeracaoPlanilhaDTO toLogDto(LogGeracaoPlanilha e) {
        return LogGeracaoPlanilhaDTO.builder()
                .id(e.getId())
                .tipo(e.getTipo())
                .chaveNfe(e.getChaveNfe())
                .numItem(e.getNumItem())
                .dataProcessamento(e.getDataProcessamento())
                .arquivoOrigem(e.getArquivoOrigem())
                .mensagem(e.getMensagem())
                .build();
    }

    private static final int ARQUIVO_ORIGEM_MAX = 500;

    private LogGeracaoPlanilha log(
            long processamentoRessarcimentoId,
            TipoLogGeracaoPlanilha tipo,
            String chave,
            Integer numItem,
            String arquivoOrigem,
            String mensagem,
            LocalDateTime data) {
        String origem = arquivoOrigem;
        if (origem != null && origem.length() > ARQUIVO_ORIGEM_MAX) {
            origem = origem.substring(0, ARQUIVO_ORIGEM_MAX - 3) + "...";
        }
        return LogGeracaoPlanilha.builder()
                .tipo(tipo.name())
                .chaveNfe(chave)
                .numItem(numItem)
                .arquivoOrigem(origem)
                .mensagem(mensagem)
                .dataProcessamento(data)
                .processamentoRessarcimento(
                        processamentoRessarcimentoRepository.getReferenceById(processamentoRessarcimentoId))
                .build();
    }

    /**
     * uCom da NF-e (Manual de Orientação Contribuinte / leiaute NF-e, grupo prod): tamanho 1 a 6 caracteres.
     */
    private static boolean uComDaNFeUtilizavel(String u) {
        if (u == null || u.isBlank()) {
            return false;
        }
        int len = u.trim().length();
        return len >= 1 && len <= 6;
    }

    private static Path exigirDiretorio(String caminho, String propriedade) {
        if (!StringUtils.hasText(caminho)) {
            throw new IllegalArgumentException("Configure a propriedade " + propriedade + " (diretório existente).");
        }
        Path p = Paths.get(caminho);
        if (!Files.isDirectory(p)) {
            throw new RecursoNaoEncontradoException("Diretório não encontrado: " + p.toAbsolutePath());
        }
        return p;
    }

    private static List<String> listarNomesEfd(Path dirEfd) throws IOException {
        try (Stream<Path> s = Files.list(dirEfd)) {
            return s.filter(ParserEfdService::isArquivoEfdCandidato)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static Path resolverArquivoResumo(Path dirResumo, Integer ano, Integer mes, String nomeArquivo)
            throws IOException {
        if (StringUtils.hasText(nomeArquivo)) {
            Path p = dirResumo.resolve(nomeArquivo.trim());
            if (!Files.isRegularFile(p)) {
                throw new RecursoNaoEncontradoException("Arquivo de resumo não encontrado: " + p.toAbsolutePath());
            }
            return p;
        }
        try (Stream<Path> stream = Files.list(dirResumo)) {
            List<Path> candidatos = stream
                    .filter(x -> Files.isRegularFile(x))
                    .filter(x -> x.getFileName().toString().toLowerCase().endsWith(".xlsx"))
                    .toList();
            if (candidatos.isEmpty()) {
                throw new RecursoNaoEncontradoException(
                        "Nenhum arquivo .xlsx encontrado em " + dirResumo.toAbsolutePath());
            }
            List<Path> filtrados = candidatos;
            if (ano != null && mes != null) {
                String sy = String.valueOf(ano);
                String sm = mes >= 1 && mes <= 9 ? "0" + mes : String.valueOf(mes);
                filtrados = candidatos.stream()
                        .filter(p -> {
                            String n = p.getFileName().toString();
                            return n.contains(sy) && n.contains(sm);
                        })
                        .toList();
                if (filtrados.isEmpty()) {
                    filtrados = candidatos;
                }
            }
            return filtrados.stream()
                    .max(Comparator.comparing(ProdutoPlanilhaAutomaticaService::ultimaModificacao))
                    .orElseThrow();
        }
    }

    private static FileTime ultimaModificacao(Path p) {
        try {
            return Files.getLastModifiedTime(p);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    private static List<ResumoNfLinhaDTO> filtrarPorPeriodo(
            List<ResumoNfLinhaDTO> linhas, Integer ano, Integer mes) {
        if (ano == null || mes == null) {
            return linhas;
        }
        return linhas.stream()
                .filter(l -> l.getDataApresentacao() != null)
                .filter(l -> l.getDataApresentacao().getYear() == ano
                        && l.getDataApresentacao().getMonthValue() == mes)
                .toList();
    }
}
