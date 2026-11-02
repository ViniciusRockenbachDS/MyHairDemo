package br.com.jofashionhair.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.jofashionhair.domain.HorarioFuncionamento;

public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Short> {

    List<HorarioFuncionamento> findAllByOrderByDiaSemanaAsc();
}
