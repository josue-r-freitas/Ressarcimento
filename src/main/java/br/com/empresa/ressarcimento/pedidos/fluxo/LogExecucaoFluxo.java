package br.com.empresa.ressarcimento.pedidos.fluxo;

import br.com.empresa.ressarcimento.processamento.domain.ProcessamentoRessarcimento;
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
@Table(name = "log_execucao_fluxo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogExecucaoFluxo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processamento_ressarcimento_id", nullable = false)
    private ProcessamentoRessarcimento processamentoRessarcimento;

    @Column(name = "nivel", nullable = false, length = 10)
    private String nivel;

    @Column(name = "etapa", nullable = false, length = 100)
    private String etapa;

    @Column(name = "mensagem", nullable = false, length = 1000)
    private String mensagem;

    @Column(name = "detalhes", columnDefinition = "NVARCHAR(MAX)")
    private String detalhes;

    @Column(name = "ts", nullable = false)
    private LocalDateTime ts;
}
