package br.com.empresa.ressarcimento.produtos.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoGeracaoPlanilhaAutomatica {

    private byte[] planilhaXlsx;
    /** Linhas únicas na planilha (após deduplicação codInterno|CNPJ|codProd|unidades). */
    private int totalProdutosGerados;
    /**
     * Linhas do resumo após filtro de período (ano/mês), antes do loop. Soma com ignoradas tributo + rejeitadas +
     * montadas = esta quantidade.
     */
    private int totalLinhasResumo;
    private int totalLogs;

    /** Fluxo A: não há filtro por TRIBUTO; mantido 0 para compatibilidade de API/cabeçalhos. */
    private int totalLinhasIgnoradasTributo;
    /** CHAVE/CNPJ/CODG inválidos, NF não na EFD, C170/0200 ausentes, etc. */
    private int totalLinhasRejeitadas;
    /** Linhas que chegaram a montar produto e participaram da deduplicação (inclui as que colapsaram). */
    private int totalLinhasMontadasParaDedup;
    /** {@code montadasParaDedup - produtos únicos}: linhas “engolidas” pela chave de dedup. */
    private int totalLinhasColapsadasNaDedup;
}
