package br.com.empresa.ressarcimento.produtos.automatizado.efd;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class NotaEfd {

    private final Map<Integer, C170Linha> itensPorNumItem = new HashMap<>();

    public void putItem(C170Linha linha) {
        itensPorNumItem.put(linha.numItem(), linha);
    }

    public Optional<C170Linha> findItem(int numItem) {
        return Optional.ofNullable(itensPorNumItem.get(numItem));
    }

    public Map<Integer, C170Linha> copyItens() {
        return new HashMap<>(itensPorNumItem);
    }
}
