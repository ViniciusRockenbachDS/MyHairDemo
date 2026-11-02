package br.com.jofashionhair.conta;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, Long> {

    /** Códigos ainda não usados da pessoa, do mais novo para o mais antigo. */
    @Query("""
            select c from CodigoVerificacao c
             where c.usuario.id = :usuarioId
               and c.finalidade in :finalidades
               and c.usadoEm is null
             order by c.id desc
            """)
    List<CodigoVerificacao> findPendentes(@Param("usuarioId") Long usuarioId,
                                          @Param("finalidades") Collection<FinalidadeCodigo> finalidades);

    /** Um código novo invalida os anteriores da mesma finalidade. */
    @Modifying(flushAutomatically = true)
    @Query("""
            update CodigoVerificacao c set c.usadoEm = :agora
             where c.usuario.id = :usuarioId and c.finalidade = :finalidade and c.usadoEm is null
            """)
    int invalidarPendentes(@Param("usuarioId") Long usuarioId,
                           @Param("finalidade") FinalidadeCodigo finalidade,
                           @Param("agora") OffsetDateTime agora);

    @Modifying
    @Query("delete from CodigoVerificacao c where c.expiraEm < :limite")
    int apagarVencidosAntesDe(@Param("limite") OffsetDateTime limite);
}
