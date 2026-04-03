package br.com.empresa.ressarcimento.planilhas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumoNfLinhaDTO {

    private int numeroLinhaPlanilha;
    private String chave;
    private int seqItem;
    private String codgItem;
    private String cnpjFornecedor;
    /** Preenchida conforme coluna DATA APRES. (usada para filtro e checagem de período). */
    private LocalDate dataApresentacao;
    /** Coluna TRIBUTO do resumonf (ex.: 1380 ICMS-ST a ressarcir). */
    private String tributo;
    /** Coluna NR. NOTA (Fluxo B / staging entrada). */
    private String nrNota;
    /** Quantidade unitária comprada — colunas mapeadas em {@code LeitorResumoNf}. */
    private BigDecimal qtdUnitCompra;
    /** Valor unitário. */
    private BigDecimal valorUnitario;
    /** CFOP (planilha). */
    private String cfop;
    /** Valor do imposto. */
    private BigDecimal valorImposto;
}
