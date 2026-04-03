package br.com.empresa.ressarcimento.produtos.automatizado;

/**
 * Campos temporais do grupo {@code ide} da NF-e (layout nacional): saída/emissão.
 * Valores como na XML (strings), sem normalização de fuso.
 */
public record NfeIdeCampos(String dhSaiEnt, String dhEmi, String dEmi) {}
