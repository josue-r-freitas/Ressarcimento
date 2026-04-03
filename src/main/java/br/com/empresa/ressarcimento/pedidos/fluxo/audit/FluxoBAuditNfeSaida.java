package br.com.empresa.ressarcimento.pedidos.fluxo.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fluxo_b_audit_nfe_saida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FluxoBAuditNfeSaida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_nfe", nullable = false, columnDefinition = "CHAR(44)")
    private String chaveNFe;

    @Column(name = "status_processamento", nullable = false, length = 40)
    private String statusProcessamento;

    @Column(name = "dh_sai_ent", length = 35)
    private String dhSaiEnt;

    @Column(name = "dh_emi", length = 35)
    private String dhEmi;

    @Column(name = "d_emi", length = 12)
    private String dEmi;

    @Column(name = "data_doc_efd")
    private LocalDate dataDocEfd;

    @Column(name = "cfops_itens_elegiveis", length = 200)
    private String cfopsItensElegiveis;
}
