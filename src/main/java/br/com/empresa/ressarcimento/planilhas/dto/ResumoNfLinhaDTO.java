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
    /** resumonf: coluna {@code QTDE. UNITÁRIA} → {@code fluxo_b_audit_item_nfe_entrada.qtd_unit_compra}. */
    private BigDecimal qtdUnitCompra;
    /** resumonf: {@code VALOR UNITÁRIO} → {@code valor_unitario}. */
    private BigDecimal valorUnitario;
    /** resumonf: {@code CODG. CFOP} → {@code cfop}. */
    private String cfop;
    /** resumonf: {@code VALOR IMPOSTO} → {@code valor_imposto}. */
    private BigDecimal valorImposto;
}
