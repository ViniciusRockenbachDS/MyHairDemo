package br.com.jofashionhair.whatsapp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.com.jofashionhair.config.AppProperties;

/**
 * Envio de mensagens pela Evolution API v2.
 * POST {base-url}/message/sendText/{instance}  header "apikey"  body {"number","text"}
 *
 * <p>Com {@code app.whatsapp.enviar-mensagens=false} (padrão no perfil dev) nada
 * é enviado: a mensagem só vai para o log.</p>
 */
@Component
public class EvolutionApiClient {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiClient.class);

    private final AppProperties.Whatsapp config;
    private final RestClient http;

    public EvolutionApiClient(AppProperties props) {
        this.config = props.whatsapp();
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        this.http = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(fabrica)
                .defaultHeader("apikey", config.apiKey())
                .build();
    }

    /** false = modo desenvolvimento: as mensagens só vão para o log. */
    public boolean estaAtivo() {
        return config.enviarMensagens();
    }

    /** @return true se a Evolution aceitou a mensagem */
    public boolean enviarTexto(String numero, String texto) {
        if (!config.enviarMensagens()) {
            log.info("[WhatsApp DESLIGADO] para {}:\n{}", numero, texto);
            return false;
        }
        try {
            http.post()
                    .uri("/message/sendText/{instance}", config.instance())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("number", numero, "text", texto))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.error("Falha ao enviar WhatsApp para {}: {}", numero, e.getMessage());
            return false;
        }
    }
}
