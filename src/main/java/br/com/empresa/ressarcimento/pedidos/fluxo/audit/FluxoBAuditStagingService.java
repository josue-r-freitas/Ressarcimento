package br.com.empresa.ressarcimento.pedidos.fluxo.audit;

import br.com.empresa.ressarcimento.planilhas.dto.ResumoNfLinhaDTO;
import br.com.empresa.ressarcimento.produtos.automatizado.LeitorNfeUcom;
import br.com.empresa.ressarcimento.produtos.automatizado.NfeIdeCampos;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staging global do Fluxo B: limpeza no início de cada {@code gerarAutomatico} e gravação das notas/itens (I) e (II).
 */
@Service
@RequiredArgsConstructor
public class FluxoBAuditStagingService {

    private final FluxoBAuditItemNfeSaidaRepository itemSaidaRepository;
    private final FluxoBAuditNfeSaidaRepository nfeSaidaRepository;
    private final FluxoBAuditItemNfeEntradaRepository itemEntradaRepository;
    private final FluxoBAuditNfeEntradaRepository nfeEntradaRepository;

    @Transactional
    public void limparStaging() {
        itemSaidaRepository.deleteAllInBatch();
        nfeSaidaRepository.deleteAllInBatch();
        itemEntradaRepository.deleteAllInBatch();
        nfeEntradaRepository.deleteAllInBatch();
    }

    @Transactional
    public FluxoBAuditNfeSaida salvarNfeSaida(FluxoBAuditNfeSaida entidade) {
        return nfeSaidaRepository.save(entidade);
    }

    @Transactional
    public void salvarItemNfeSaida(FluxoBAuditItemNfeSaida item) {
        itemSaidaRepository.save(item);
    }

    /**
     * Persiste resumo NF (II): uma nota por chave 44 distinta; uma linha de item por linha da planilha filtrada.
     */
    @Transactional
    public void persistirEntradasDoResumo(List<ResumoNfLinhaDTO> linhasFiltradas, Path dirNfeEntrada, LeitorNfeUcom leitor)
            throws Exception {
        Map<String, List<ResumoNfLinhaDTO>> porChave = linhasFiltradas.stream()
                .filter(l -> l.getChave() != null && l.getChave().length() == 44)
                .collect(Collectors.groupingBy(ResumoNfLinhaDTO::getChave, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<ResumoNfLinhaDTO>> e : porChave.entrySet()) {
            String chave = e.getKey();
            ResumoNfLinhaDTO primeira = e.getValue().get(0);
            FluxoBAuditNfeEntrada hdr = FluxoBAuditNfeEntrada.builder()
                    .chaveNFe(chave)
                    .nrNota(primeira.getNrNota())
                    .dataApresentacao(primeira.getDataApresentacao())
                    .build();

            Optional<Path> xmlOpt = leitor.localizarArquivoXml(dirNfeEntrada, chave);
            if (xmlOpt.isPresent()) {
                Optional<NfeIdeCampos> ide = leitor.lerEmissaoIdeEntrada(xmlOpt.get());
                if (ide.isPresent()) {
                    NfeIdeCampos c = ide.get();
                    if (c.dhEmi() != null) {
                        hdr.setDhEmi(trunc(c.dhEmi(), 35));
                    } else if (c.dEmi() != null) {
                        hdr.setDEmi(trunc(c.dEmi(), 12));
                    }
                }
            }

            hdr = nfeEntradaRepository.save(hdr);
            for (ResumoNfLinhaDTO lin : e.getValue()) {
                FluxoBAuditItemNfeEntrada item = FluxoBAuditItemNfeEntrada.builder()
                        .auditNfeEntrada(hdr)
                        .seqItem(lin.getSeqItem())
                        .codgItem(lin.getCodgItem())
                        .tributo(lin.getTributo())
                        .qtdUnitCompra(lin.getQtdUnitCompra())
                        .valorUnitario(lin.getValorUnitario())
                        .cfop(normalizarCfop4(lin.getCfop()))
                        .valorImposto(lin.getValorImposto())
                        .cnpjFornecedor(normalizarCnpj14(lin.getCnpjFornecedor()))
                        .numeroLinhaPlanilha(lin.getNumeroLinhaPlanilha())
                        .build();
                itemEntradaRepository.save(item);
            }
        }
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String normalizarCfop4(String cfop) {
        if (cfop == null || cfop.isBlank()) {
            return null;
        }
        String d = cfop.replaceAll("\\D", "");
        if (d.isEmpty()) {
            return null;
        }
        if (d.length() >= 4) {
            return d.substring(0, 4);
        }
        return String.format("%4s", d).replace(' ', '0');
    }

    private static String normalizarCnpj14(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }
        String d = cnpj.replaceAll("\\D", "");
        return d.length() == 14 ? d : null;
    }
}
