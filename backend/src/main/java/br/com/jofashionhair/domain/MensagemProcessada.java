package br.com.jofashionhair.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Id de mensagem do WhatsApp já processada (idempotência do webhook). */
@Entity
@Table(name = "whatsapp_mensagens_processadas")
public class MensagemProcessada {

    @Id
    @Column(name = "message_id", length = 100)
    private String messageId;

    @Column(name = "recebida_em", nullable = false)
    private OffsetDateTime recebidaEm;

    protected MensagemProcessada() {
    }

    public String getMessageId() { return messageId; }
    public OffsetDateTime getRecebidaEm() { return recebidaEm; }
}
