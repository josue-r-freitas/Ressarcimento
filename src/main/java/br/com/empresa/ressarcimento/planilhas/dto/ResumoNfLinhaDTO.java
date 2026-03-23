package br.com.empresa.ressarcimento.planilhas.dto;

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
}
