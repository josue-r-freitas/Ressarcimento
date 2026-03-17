package br.com.empresa.ressarcimento.produtos.domain;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "arquivo_produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArquivoProdutos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declarante_id", nullable = false)
    private Declarante declarante;

    @Column(name = "data_geracao", nullable = false)
    private LocalDateTime dataGeracao;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "mensagem_log", columnDefinition = "NVARCHAR(MAX)")
    private String mensagemLog;

    @Column(name = "xml_content", columnDefinition = "NVARCHAR(MAX)")
    private String xmlContent;
}
