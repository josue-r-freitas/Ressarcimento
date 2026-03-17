package br.com.empresa.ressarcimento.pedidos;

import br.com.empresa.ressarcimento.pedidos.domain.NotaEntrada;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaEntradaRepository extends JpaRepository<NotaEntrada, Long> {

    Optional<NotaEntrada> findByChaveNFeEntrada(String chaveNFeEntrada);
}
