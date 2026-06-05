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

    public Optional<C170Linha> findItem(int numItem, String codInterno) {
        return findItem(numItem).filter(c ->
                codInterno != null && codInterno.trim().equalsIgnoreCase(c.codItem().trim()));
    }

    public Map<Integer, C170Linha> copyItens() {
        return new HashMap<>(itensPorNumItem);
    }
}
