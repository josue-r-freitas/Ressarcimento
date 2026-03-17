package br.com.empresa.ressarcimento.declarante;

import br.com.empresa.ressarcimento.declarante.domain.Declarante;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeclaranteRepository extends JpaRepository<Declarante, Long> {

    Optional<Declarante> findByCnpjRaiz(String cnpjRaiz);
}
