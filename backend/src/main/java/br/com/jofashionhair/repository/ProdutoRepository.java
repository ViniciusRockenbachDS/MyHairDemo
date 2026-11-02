package br.com.jofashionhair.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.jofashionhair.domain.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /** Vitrine do site. */
    List<Produto> findByAtivoTrueOrderByOrdemAscNomeAsc();

    /** Painel: visíveis primeiro. */
    List<Produto> findAllByOrderByAtivoDescOrdemAscNomeAsc();
}
