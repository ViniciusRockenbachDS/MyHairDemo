package br.com.jofashionhair.pagamento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.jofashionhair.config.AppProperties;

/**
 * Pix com confirmação automática pelo Mercado Pago.
 *
 * <ul>
 *   <li>{@link #criarPix}: cria um pagamento Pix e devolve o QR Code.</li>
 *   <li>{@link #consultar}: confere o pagamento direto no Mercado Pago (o aviso
 *       do webhook nunca é aceito sem esta conferência).</li>
 *   <li>{@link #assinaturaValida}: confere a assinatura do aviso (x-signature).</li>
 * </ul>
 */
@Component
public class MercadoPagoClient {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public record Consulta(String status, String referencia) {
        public boolean aprovado() {
            return "approved".equals(status);
        }
    }

    private final AppProperties.Pagamentos config;
    private final String urlAviso;
    private final RestClient http;

    public MercadoPagoClient(AppProperties props) {
        this.config = props.pagamentos();
        String site = props.siteUrl() == null ? "" : props.siteUrl().replaceAll("/+$", "");
        this.urlAviso = site + "/api/webhooks/mercadopago";
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        this.http = RestClient.builder().baseUrl("https://api.mercadopago.com").requestFactory(fabrica).build();
    }

    /** Configurado = token de acesso e e-mail do pagador informados. */
    public boolean configurado() {
        return config != null && temTexto(config.mercadopagoToken()) && temTexto(config.mercadopagoEmailPagador());
    }

    public PixGerado criarPix(BigDecimal valor, String descricao, String referencia, OffsetDateTime expiraEm) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("transaction_amount", valor);
        corpo.put("description", descricao);
        corpo.put("payment_method_id", "pix");
        corpo.put("external_reference", referencia);
        corpo.put("date_of_expiration", ISO.format(expiraEm));
        corpo.put("payer", Map.of("email", config.mercadopagoEmailPagador()));
        if (urlAviso.startsWith("https://")) {
            corpo.put("notification_url", urlAviso);   // o Mercado Pago só avisa endereços públicos com HTTPS
        }
        JsonNode r = http.post()
                .uri("/v1/payments")
                .header("Authorization", "Bearer " + config.mercadopagoToken())
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);
        if (r == null) {
            throw new IllegalStateException("Resposta vazia do Mercado Pago");
        }
        JsonNode dados = r.path("point_of_interaction").path("transaction_data");
        return new PixGerado(r.path("id").asText(), dados.path("qr_code").asText(), dados.path("qr_code_base64").asText());
    }

    public Consulta consultar(String pagamentoId) {
        JsonNode r = http.get()
                .uri("/v1/payments/{id}", pagamentoId)
                .header("Authorization", "Bearer " + config.mercadopagoToken())
                .retrieve()
                .body(JsonNode.class);
        return r == null ? new Consulta(null, null)
                : new Consulta(r.path("status").asText(null), r.path("external_reference").asText(null));
    }

    /**
     * Confere o cabeçalho x-signature ("ts=...,v1=...") com a chave secreta do
     * webhook. Sem chave configurada, aceita (a consulta ao Mercado Pago já
     * impede avisos falsos de pagamento).
     */
    public boolean assinaturaValida(String xSignature, String xRequestId, String dataId) {
        if (config == null || !temTexto(config.mercadopagoWebhookSecret())) {
            return true;
        }
        if (xSignature == null) {
            return true;    // sem assinatura: o pagamento é conferido direto no Mercado Pago de qualquer forma
        }
        if (dataId == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("ts")) ts = kv[1];
            if (kv.length == 2 && kv[0].equals("v1")) v1 = kv[1];
        }
        if (ts == null || v1 == null) {
            return false;
        }
        String manifesto = "id:" + dataId.toLowerCase() + ";" + (xRequestId != null ? "request-id:" + xRequestId + ";" : "")
                + "ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.mercadopagoWebhookSecret().getBytes(), "HmacSHA256"));
            byte[] h = mac.doFinal(manifesto.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : h) hex.append(String.format("%02x", b));
            return java.security.MessageDigest.isEqual(hex.toString().getBytes(), v1.getBytes());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }
}
