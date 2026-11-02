package br.com.jofashionhair.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.jofashionhair.domain.Profissional;
import jakarta.persistence.LockModeType;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    /**
     * SELECT ... FOR UPDATE na linha da profissional.
     *
     * <p>Serializa as reservas (e bloqueios) da MESMA profissional: enquanto uma
     * transação verifica e grava, outra que mexa na agenda dela espera.
     * Profissionais diferentes seguem em paralelo.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profissional p where p.id = :id")
    Optional<Profissional> findByIdParaReserva(@Param("id") Long id);

    /** Profissionais ativas que realizam o serviço, em ordem estável (por id). */
    @Query("""
            select p from Profissional p
              join p.servicos s
             where s.id = :servicoId
               and p.ativo = true
             order by p.id
            """)
    List<Profissional> findAtivasQueRealizam(@Param("servicoId") Long servicoId);

    @Query("""
            select count(p) > 0 from Profissional p
              join p.servicos s
             where p.id = :profissionalId
               and s.id = :servicoId
            """)
    boolean realizaServico(@Param("profissionalId") Long profissionalId,
                           @Param("servicoId") Long servicoId);

    List<Profissional> findByAtivoTrueOrderByNomeAsc();

    /** Lista do /admin já com os serviços vinculados (evita N+1). */
    @Query("select distinct p from Profissional p left join fetch p.servicos order by p.nome")
    List<Profissional> findAllComServicos();

    @Query("select p from Profissional p left join fetch p.servicos where p.id = :id")
    Optional<Profissional> findByIdComServicos(@Param("id") Long id);
}
