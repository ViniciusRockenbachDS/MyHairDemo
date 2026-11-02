package br.com.jofashionhair.whatsapp;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Mensagem de texto recebida de um cliente, já extraída do payload da Evolution API.
 *
 * @param messageId   id da mensagem no WhatsApp (idempotência)
 * @param numero      número do remetente exatamente como veio do WhatsApp (usado para responder)
 * @param nomeContato nome do perfil do WhatsApp (pushName)
 * @param texto       conteúdo; vazio para áudio, figurinha etc.
 */
public record MensagemRecebida(String messageId, String numero, String nomeContato, String texto) {

    /**
     * Lê um evento "messages.upsert" da Evolution API v2. Ignora (Optional vazio):
     * outros eventos, mensagens enviadas pelo próprio salão, grupos, status e broadcast.
     */
    public static Optional<MensagemRecebida> doPayloadEvolution(JsonNode raiz) {
        String evento = raiz.path("event").asText("").toLowerCase().replace('_', '.');
        if (!"messages.upsert".equals(evento)) {
            return Optional.empty();
        }
        JsonNode data = raiz.path("data");
        if (data.isArray()) {
            data = data.path(0);
        }
        JsonNode key = data.path("key");
        if (key.path("fromMe").asBoolean(false)) {
            return Optional.empty();
        }

        String jid = key.path("remoteJid").asText("");
        if (jid.endsWith("@lid")) {
            // contas com identificador "LID": o número real vem num campo alternativo
            jid = primeiroPreenchido(key.path("remoteJidAlt").asText(""), key.path("senderPn").asText(""),
                    data.path("senderPn").asText(""));
        }
        if (!jid.endsWith("@s.whatsapp.net")) {
            return Optional.empty();   // grupo (@g.us), status@broadcast, newsletter...
        }
        String numero = jid.substring(0, jid.indexOf('@'));
        if (numero.contains(":")) {
            numero = numero.substring(0, numero.indexOf(':'));   // sufixo de dispositivo
        }

        JsonNode m = data.path("message");
        String texto = primeiroPreenchido(
                m.path("conversation").asText(""),
                m.path("extendedTextMessage").path("text").asText(""),
                m.path("buttonsResponseMessage").path("selectedDisplayText").asText(""),
                m.path("listResponseMessage").path("title").asText(""),
                m.path("imageMessage").path("caption").asText(""));

        String id = key.path("id").asText("");
        return Optional.of(new MensagemRecebida(id.isBlank() ? null : id, numero,
                data.path("pushName").asText(""), texto));
    }

    private static String primeiroPreenchido(String... valores) {
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
