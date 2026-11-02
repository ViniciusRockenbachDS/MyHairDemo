package br.com.jofashionhair.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.jofashionhair.config.AppProperties;

/**
 * Webhook da Evolution API.
 *
 * <p>Configure na Evolution a URL:
 * {@code https://SEU-DOMINIO/api/webhooks/whatsapp/{WEBHOOK_SECRET}}
 * com o evento MESSAGES_UPSERT. O segredo na URL impede que terceiros
 * enviem mensagens falsas ao robô.</p>
 *
 * <p>Sempre responde 200 rapidamente para eventos válidos (mesmo os ignorados),
 * senão a Evolution fica reenviando.</p>
 */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);

    private final AtendimentoWhatsappService atendimento;
    private final byte[] segredo;

    public WhatsappWebhookController(AtendimentoWhatsappService atendimento, AppProperties props) {
        this.atendimento = atendimento;
        String s = props.whatsapp().webhookSecret();
        this.segredo = s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
        if ("troque-me".equals(s)) {
            log.warn("WEBHOOK_SECRET está com o valor padrão. Defina um segredo forte em produção!");
        }
    }

    @PostMapping("/{token}")
    public ResponseEntity<Void> receber(@PathVariable String token, @RequestBody JsonNode payload) {
        if (segredo.length == 0 || !MessageDigest.isEqual(segredo, token.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(401).build();
        }
        MensagemRecebida.doPayloadEvolution(payload).ifPresent(atendimento::processarMensagemRecebida);
        return ResponseEntity.ok().build();
    }
}
