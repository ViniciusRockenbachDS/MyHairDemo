package br.com.jofashionhair.repository;

import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.MensagemProcessada;

public interface MensagemProcessadaRepository extends JpaRepository<MensagemProcessada, String> {

    /**
     * Registra o id de forma atômica.
     * @return 1 se é a primeira vez que a mensagem chega; 0 se é repetida
     */
    @Transactional
    @Modifying
    @Query(value = "insert into whatsapp_mensagens_processadas (message_id) values (:id) on conflict do nothing",
            nativeQuery = true)
    int registrar(@Param("id") String messageId);

    @Transactional
    @Modifying
    @Query("delete from MensagemProcessada m where m.recebidaEm < :limite")
    int apagarAntesDe(@Param("limite") OffsetDateTime limite);
}
