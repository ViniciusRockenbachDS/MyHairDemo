package br.com.jofashionhair.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.jofashionhair.domain.Avaliacao;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    /** Visíveis no site: pela ordem escolhida, depois as mais recentes. */
    List<Avaliacao> findByAtivoTrueOrderByOrdemAscDataDescIdDesc();

    List<Avaliacao> findAllByOrderByOrdemAscDataDescIdDesc();
}
