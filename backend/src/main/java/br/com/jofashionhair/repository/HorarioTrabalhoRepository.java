package br.com.jofashionhair.repository;

import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.jofashionhair.domain.HorarioTrabalho;

public interface HorarioTrabalhoRepository extends JpaRepository<HorarioTrabalho, Long> {

    List<HorarioTrabalho> findByProfissionalIdAndDiaSemanaOrderByHoraInicio(Long profissionalId, Short diaSemana);

    List<HorarioTrabalho> findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(Long profissionalId);

    /**
     * true se alguma faixa de expediente do dia cobre INTEIRAMENTE o intervalo
     * [inicio, fim]. Ex.: faixa 13:30-19:00 cobre 17:00-18:00, mas não 18:30-19:30.
     */
    @Query("""
            select count(h) > 0 from HorarioTrabalho h
             where h.profissional.id = :profissionalId
               and h.diaSemana = :diaSemana
               and h.horaInicio <= :inicio
               and h.horaFim >= :fim
            """)
    boolean cobreIntervalo(@Param("profissionalId") Long profissionalId,
                           @Param("diaSemana") Short diaSemana,
                           @Param("inicio") LocalTime inicio,
                           @Param("fim") LocalTime fim);

    /**
     * DELETE em lote, executado na hora. (Um deleteAll() comum deixaria o
     * Hibernate fazer os INSERTs antes dos DELETEs no flush e estourar a
     * constraint única ao substituir a grade.)
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from HorarioTrabalho h where h.profissional.id = :profissionalId")
    void apagarDaProfissional(@Param("profissionalId") Long profissionalId);
}
