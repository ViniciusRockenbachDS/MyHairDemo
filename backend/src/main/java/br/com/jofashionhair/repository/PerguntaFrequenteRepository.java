package br.com.jofashionhair.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.jofashionhair.domain.PerguntaFrequente;

public interface PerguntaFrequenteRepository extends JpaRepository<PerguntaFrequente, Long> {

    List<PerguntaFrequente> findByAtivoTrueOrderByOrdemAscIdAsc();

    List<PerguntaFrequente> findAllByOrderByOrdemAscIdAsc();
}
