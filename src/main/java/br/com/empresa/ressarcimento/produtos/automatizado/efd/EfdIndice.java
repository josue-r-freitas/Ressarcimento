package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Índices após leitura do SPED: NF-e de entrada (C100 IND_OPER=0, MOD=55), saída (IND_OPER=1, MOD=55), 0200/0220/0190.
 */
public class EfdIndice {

    private final Map<String, NotaEfd> notasEntradaPorChave = new HashMap<>();
    private final Map<String, NotaEfd> notasSaidaPorChave = new HashMap<>();
    private final Map<String, LocalDate> dataDocumentoSaidaPorChave = new HashMap<>();
    private final Map<String, InfoItemSped> itens0200PorCodItem = new HashMap<>();
    private final Map<String, String> unidades0190 = new HashMap<>();

    public void mergeNotaEntrada(String chave44, NotaEfd notaNova) {
        merge(notasEntradaPorChave, chave44, notaNova);
    }

    public void mergeNotaSaida(String chave44, NotaEfd notaNova, LocalDate dataDocumento) {
        merge(notasSaidaPorChave, chave44, notaNova);
        if (dataDocumento != null) {
            dataDocumentoSaidaPorChave.putIfAbsent(chave44, dataDocumento);
        }
    }

    private static void merge(Map<String, NotaEfd> mapa, String chave44, NotaEfd notaNova) {
        NotaEfd dest = mapa.computeIfAbsent(chave44, k -> new NotaEfd());
        for (Map.Entry<Integer, C170Linha> e : notaNova.copyItens().entrySet()) {
            dest.putItem(e.getValue());
        }
    }

    /** NF-e de entrada (C100 entrada modelo 55). */
    public Optional<NotaEfd> notaEntradaPorChave(String chave44) {
        return Optional.ofNullable(notasEntradaPorChave.get(chave44));
    }

    /** @deprecated use {@link #notaEntradaPorChave(String)} */
    @Deprecated
    public Optional<NotaEfd> notaPorChave(String chave44) {
        return notaEntradaPorChave(chave44);
    }

    public Optional<NotaEfd> notaSaidaPorChave(String chave44) {
        return Optional.ofNullable(notasSaidaPorChave.get(chave44));
    }

    public Optional<LocalDate> dataDocumentoSaida(String chave44) {
        return Optional.ofNullable(dataDocumentoSaidaPorChave.get(chave44));
    }

    public java.util.List<String> chavesSaidaNoMes(int ano, int mes) {
        return dataDocumentoSaidaPorChave.entrySet().stream()
                .filter(e -> e.getValue() != null
                        && e.getValue().getYear() == ano
                        && e.getValue().getMonthValue() == mes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Optional<InfoItemSped> infoItem(String codItem) {
        return Optional.ofNullable(itens0200PorCodItem.get(codItem));
    }

    public void putInfoItem(String codItem, InfoItemSped info) {
        itens0200PorCodItem.put(codItem, info);
    }

    public void putUnidade0190(String unid, String descr) {
        if (unid != null && !unid.isBlank()) {
            unidades0190.put(unid.trim(), descr != null ? descr : "");
        }
    }

    public boolean existeUnidade0190(String unid) {
        return unid != null && unidades0190.containsKey(unid.trim());
    }
}
