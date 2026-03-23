package br.com.empresa.ressarcimento.produtos.automatizado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "log_geracao_planilha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogGeracaoPlanilha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(name = "chave_nfe", columnDefinition = "CHAR(44)")
    private String chaveNfe;

    @Column(name = "num_item")
    private Integer numItem;

    @Column(name = "data_processamento", nullable = false)
    private LocalDateTime dataProcessamento;

    @Column(name = "arquivo_origem", length = 500)
    private String arquivoOrigem;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String mensagem;
}
