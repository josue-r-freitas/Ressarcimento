package br.com.empresa.ressarcimento.pedidos.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEntradaConsumidaDTO {

    private String chaveNfeEntrada;
    private Integer seqItem;
    private String codgItem;
    private String cnpjFornecedor;
    private String tributo;
    private LocalDate dataApresentacao;
    private BigDecimal quantidadeOriginalUnidadeFornecedor;
    private String unidadeFornecedor;
    private BigDecimal fatorConversaoAplicado;
    private BigDecimal quantidadeConvertidaUnidadeInterna;
    private BigDecimal quantidadeConsumida;
    private boolean conversaoAplicada;
}
