package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.pedidos.domain.NotaSaida;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaSaidaRepository extends JpaRepository<NotaSaida, Long> {

    Optional<NotaSaida> findByChaveNFe(String chaveNFe);

    List<NotaSaida> findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
            Long declaranteId, String ano, String mes);

    Page<NotaSaida> findByDeclaranteIdAndAnoPeriodoReferenciaAndMesPeriodoReferencia(
            Long declaranteId, String ano, String mes, Pageable pageable);

    Page<NotaSaida> findByDeclaranteId(Long declaranteId, Pageable pageable);
}
