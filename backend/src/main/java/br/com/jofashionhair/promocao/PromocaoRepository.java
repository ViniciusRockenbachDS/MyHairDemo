package br.com.jofashionhair.promocao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PromocaoRepository extends JpaRepository<Promocao, Long> {

    @Query("select distinct p from Promocao p left join fetch p.servicos order by p.ativo desc, p.id desc")
    List<Promocao> findAllComServicos();

    @Query("select distinct p from Promocao p left join fetch p.servicos where p.ativo = true")
    List<Promocao> findAtivasComServicos();
}
