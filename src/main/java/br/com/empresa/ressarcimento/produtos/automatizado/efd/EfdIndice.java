package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Índices para consulta rápida após leitura dos arquivos EFD (Bloco 0 e Bloco C).
 */
public class EfdIndice {

    private final Map<String, NotaEfd> notasPorChave = new HashMap<>();
    private final Map<String, InfoItemSped> itens0200PorCodItem = new HashMap<>();

    public void mergeNota(String chave44, NotaEfd notaNova) {
        NotaEfd dest = notasPorChave.computeIfAbsent(chave44, k -> new NotaEfd());
        for (Map.Entry<Integer, C170Linha> e : notaNova.copyItens().entrySet()) {
            dest.putItem(e.getValue());
        }
    }

    public Optional<NotaEfd> notaPorChave(String chave44) {
        return Optional.ofNullable(notasPorChave.get(chave44));
    }

    public Optional<InfoItemSped> infoItem(String codItem) {
        return Optional.ofNullable(itens0200PorCodItem.get(codItem));
    }

    public void putInfoItem(String codItem, InfoItemSped info) {
        itens0200PorCodItem.put(codItem, info);
    }
}
