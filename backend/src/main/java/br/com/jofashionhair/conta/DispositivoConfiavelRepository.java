package br.com.jofashionhair.conta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DispositivoConfiavelRepository extends JpaRepository<DispositivoConfiavel, Long> {

    @Query("select d from DispositivoConfiavel d join fetch d.usuario where d.tokenHash = :hash")
    Optional<DispositivoConfiavel> findByTokenHash(@Param("hash") String tokenHash);

    List<DispositivoConfiavel> findByUsuarioIdOrderByUltimoUsoEmDesc(Long usuarioId);

    /** Desconecta todos os dispositivos da pessoa, menos o informado (pode ser "" para todos). */
    @Modifying
    @Query("delete from DispositivoConfiavel d where d.usuario.id = :usuarioId and d.tokenHash <> :manterHash")
    int removerTodosExceto(@Param("usuarioId") Long usuarioId, @Param("manterHash") String manterHash);

    @Modifying
    @Query("delete from DispositivoConfiavel d where d.expiraEm < :agora")
    int removerVencidos(@Param("agora") OffsetDateTime agora);
}
