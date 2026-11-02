package br.com.jofashionhair.service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * "Tempo real" do site público via Server-Sent Events (SSE).
 *
 * <p>O site abre {@code new EventSource('/api/public/eventos')}; quando o admin
 * altera preço/duração/agenda, todos os navegadores conectados recebem o evento
 * {@code catalogo-atualizado} e recarregam os dados. O EventSource reconecta
 * sozinho se a conexão cair.</p>
 *
 * <p>Obs.: as conexões ficam na memória desta instância. Se um dia o sistema
 * rodar com várias instâncias, troque por Redis Pub/Sub ou PostgreSQL LISTEN/NOTIFY.</p>
 */
@Component
public class CatalogoEventos {

    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    public SseEmitter inscrever() {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(erro -> emitters.remove(emitter));
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void aoAlterarCatalogo(CatalogoAlteradoEvent evento) {
        enviar(() -> SseEmitter.event()
                .name("catalogo-atualizado")
                .data(Map.of("tipo", evento.tipo())));
    }

    /** Mantém a conexão viva atrás de proxies que derrubam conexões ociosas. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        if (!emitters.isEmpty()) {
            enviar(() -> SseEmitter.event().comment("ping"));
        }
    }

    public int conexoesAtivas() {
        return emitters.size();
    }

    private void enviar(Supplier<SseEmitter.SseEventBuilder> evento) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(evento.get());   // um builder novo por conexão
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
