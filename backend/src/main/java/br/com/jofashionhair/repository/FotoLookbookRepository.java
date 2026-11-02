package br.com.jofashionhair.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.jofashionhair.domain.FotoLookbook;

public interface FotoLookbookRepository extends JpaRepository<FotoLookbook, Long> {

    List<FotoLookbook> findByAtivoTrueOrderByOrdemAscIdDesc();

    List<FotoLookbook> findAllByOrderByAtivoDescOrdemAscIdDesc();
}
