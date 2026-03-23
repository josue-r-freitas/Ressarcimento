package br.com.empresa.ressarcimento.produtos.automatizado;

import br.com.empresa.ressarcimento.produtos.automatizado.domain.LogGeracaoPlanilha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogGeracaoPlanilhaRepository extends JpaRepository<LogGeracaoPlanilha, Long> {

    Page<LogGeracaoPlanilha> findAllByOrderByDataProcessamentoDesc(Pageable pageable);
}
