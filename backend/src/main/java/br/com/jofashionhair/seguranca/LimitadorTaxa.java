package br.com.jofashionhair.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Contador de tentativas por chave (ex.: "login:189.10.20.30") em janela fixa.
 * Fica em memória: simples e suficiente para uma instância da aplicação.
 */
@Component
public class LimitadorTaxa {

    private record Janela(Instant inicio, Duration duracao, int contagem) {
        boolean expirou(Instant agora) {
            return agora.isAfter(inicio.plus(duracao));
        }
    }

    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();

    /** Registra uma ocorrência. @return quantas já houve dentro da janela atual (incluindo esta). */
    public int registrar(String chave, Duration duracao) {
        Instant agora = Instant.now();
        return janelas.compute(chave, (k, j) -> j == null || j.expirou(agora)
                ? new Janela(agora, duracao, 1)
                : new Janela(j.inicio(), j.duracao(), j.contagem() + 1)).contagem();
    }

    /** Quantas ocorrências há na janela atual (0 se não há ou já expirou). */
    public int contagem(String chave) {
        Janela j = janelas.get(chave);
        return j == null || j.expirou(Instant.now()) ? 0 : j.contagem();
    }

    /** Segundos até a janela da chave terminar. */
    public long segundosRestantes(String chave) {
        Janela j = janelas.get(chave);
        if (j == null) {
            return 0;
        }
        return Math.max(1, Duration.between(Instant.now(), j.inicio().plus(j.duracao())).getSeconds());
    }

    public void limpar(String chave) {
        janelas.remove(chave);
    }

    /** A cada 10 minutos, descarta janelas vencidas (evita acumular memória). */
    @Scheduled(fixedRate = 600_000)
    public void descartarVencidas() {
        Instant agora = Instant.now();
        janelas.values().removeIf(j -> j.expirou(agora));
    }
}
