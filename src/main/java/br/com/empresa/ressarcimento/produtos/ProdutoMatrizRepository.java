package br.com.empresa.ressarcimento.produtos;

import br.com.empresa.ressarcimento.produtos.domain.ProdutoMatriz;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoMatrizRepository extends JpaRepository<ProdutoMatriz, Long> {

    Page<ProdutoMatriz> findByCodInternoProdutoContainingOrDescricaoProdutoContaining(
            String codigo, String descricao, Pageable pageable);

    List<ProdutoMatriz> findAllByOrderByCodInternoProduto();

    boolean existsByCodInternoProdutoAndCnpjFornecedorAndCodProdFornecedor(
            String codInterno, String cnpjFornecedor, String codProdFornecedor);

    Optional<ProdutoMatriz> findByCodInternoProdutoAndCnpjFornecedorAndCodProdFornecedor(
            String codInterno, String cnpjFornecedor, String codProdFornecedor);

    boolean existsByCodInternoProduto(String codInternoProduto);

    List<ProdutoMatriz> findByCodInternoProduto(String codInternoProduto);
}
