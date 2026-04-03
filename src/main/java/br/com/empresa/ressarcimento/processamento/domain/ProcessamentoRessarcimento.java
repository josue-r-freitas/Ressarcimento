package br.com.empresa.ressarcimento.processamento.domain;

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
@Table(name = "processamento_ressarcimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessamentoRessarcimento {

    public static final String STATUS_EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String STATUS_CONCLUIDO = "CONCLUIDO";
    public static final String STATUS_ERRO = "ERRO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declarante_id", nullable = false)
    private Declarante declarante;

    @Column(name = "ano_referencia", nullable = false, columnDefinition = "CHAR(4)")
    private String anoReferencia;

    @Column(name = "mes_referencia", nullable = false, columnDefinition = "CHAR(2)")
    private String mesReferencia;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim")
    private LocalDateTime dataHoraFim;

    @Column(name = "status_execucao", nullable = false, length = 40)
    private String statusExecucao;

    @Column(name = "mensagem_erro", length = 2000)
    private String mensagemErro;
}
