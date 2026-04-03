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
@Table(name = "fluxo_b_audit_nfe_entrada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FluxoBAuditNfeEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave_nfe", nullable = false, unique = true, columnDefinition = "CHAR(44)")
    private String chaveNFe;

    @Column(name = "nr_nota", length = 20)
    private String nrNota;

    @Column(name = "data_apresentacao")
    private LocalDate dataApresentacao;

    @Column(name = "dh_emi", length = 35)
    private String dhEmi;

    @Column(name = "d_emi", length = 12)
    private String dEmi;
}
