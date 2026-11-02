package br.com.jofashionhair.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.jofashionhair.domain.BloqueioAgenda;

public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {

    /** Sobreposição de intervalos semiabertos: inicioA < fimB  E  fimA > inicioB. */
    @Query("""
            select count(b) > 0 from BloqueioAgenda b
             where b.profissional.id = :profissionalId
               and b.dataInicio < :fim
               and b.dataFim > :inicio
            """)
    boolean existeSobreposicao(@Param("profissionalId") Long profissionalId,
                               @Param("inicio") OffsetDateTime inicio,
                               @Param("fim") OffsetDateTime fim);

    @Query("""
            select b from BloqueioAgenda b
             where b.profissional.id = :profissionalId
               and b.dataInicio < :fim
               and b.dataFim > :inicio
             order by b.dataInicio
            """)
    List<BloqueioAgenda> findSobrepostos(@Param("profissionalId") Long profissionalId,
                                         @Param("inicio") OffsetDateTime inicio,
                                         @Param("fim") OffsetDateTime fim);

    /** Bloqueios de todas as profissionais num período (tela do /admin). */
    @Query("""
            select b from BloqueioAgenda b
              join fetch b.profissional
             where b.dataInicio < :fim
               and b.dataFim > :inicio
             order by b.dataInicio
            """)
    List<BloqueioAgenda> findPeriodoComProfissional(@Param("inicio") OffsetDateTime inicio,
                                                    @Param("fim") OffsetDateTime fim);
}
