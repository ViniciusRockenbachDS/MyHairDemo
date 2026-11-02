package br.com.jofashionhair.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.jofashionhair.domain.AtendimentoWhatsappEstado;
import br.com.jofashionhair.domain.EstadoConversa;

public interface AtendimentoWhatsappEstadoRepository extends JpaRepository<AtendimentoWhatsappEstado, Long> {

    Optional<AtendimentoWhatsappEstado> findByTelefone(String telefone);

    List<AtendimentoWhatsappEstado> findByEstadoOrderByUltimaInteracaoEmDesc(EstadoConversa estado);

    List<AtendimentoWhatsappEstado> findTop100ByOrderByUltimaInteracaoEmDesc();

    @Modifying
    @Query("delete from AtendimentoWhatsappEstado e where e.telefone = :telefone")
    int apagarPorTelefone(@Param("telefone") String telefone);

    /** Conversas paradas há muito tempo não precisam ficar guardadas. */
    @Modifying
    @Query("delete from AtendimentoWhatsappEstado e where e.ultimaInteracaoEm < :limite")
    int apagarInativasDesde(@Param("limite") OffsetDateTime limite);
}
