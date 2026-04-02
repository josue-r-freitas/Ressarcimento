package br.com.empresa.ressarcimento.produtos.api;

import org.springframework.http.HttpHeaders;

/**
 * Cabeçalhos HTTP com totais da geração (útil no download da UI e na API: DevTools → Network → Response Headers).
 */
public final class PlanilhaAutomaticaMetricasHeaders {

    public static final String LINHAS_RESUMO_PERIODO = "X-Ressarcimento-Linhas-Resumo-Periodo";
    public static final String LINHAS_IGNORADAS_TRIBUTO = "X-Ressarcimento-Linhas-Ignoradas-Tributo";
    public static final String LINHAS_REJEITADAS = "X-Ressarcimento-Linhas-Rejeitadas";
    public static final String LINHAS_MONTADAS_DEDUP = "X-Ressarcimento-Linhas-Montadas-Dedup";
    public static final String LINHAS_COLAPSADAS_DEDUP = "X-Ressarcimento-Linhas-Colapsadas-Dedup";
    public static final String PRODUTOS_UNICOS = "X-Ressarcimento-Produtos-Unicos";
    public static final String TOTAL_LOGS = "X-Ressarcimento-Total-Logs";

    private PlanilhaAutomaticaMetricasHeaders() {}

    public static void addTo(HttpHeaders headers, ResultadoGeracaoPlanilhaAutomatica r) {
        if (r == null) {
            return;
        }
        headers.add(LINHAS_RESUMO_PERIODO, String.valueOf(r.getTotalLinhasResumo()));
        headers.add(LINHAS_IGNORADAS_TRIBUTO, String.valueOf(r.getTotalLinhasIgnoradasTributo()));
        headers.add(LINHAS_REJEITADAS, String.valueOf(r.getTotalLinhasRejeitadas()));
        headers.add(LINHAS_MONTADAS_DEDUP, String.valueOf(r.getTotalLinhasMontadasParaDedup()));
        headers.add(LINHAS_COLAPSADAS_DEDUP, String.valueOf(r.getTotalLinhasColapsadasNaDedup()));
        headers.add(PRODUTOS_UNICOS, String.valueOf(r.getTotalProdutosGerados()));
        headers.add(TOTAL_LOGS, String.valueOf(r.getTotalLogs()));
    }
}
