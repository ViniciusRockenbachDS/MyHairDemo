package br.com.jofashionhair.promocao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import br.com.jofashionhair.service.RelogioSalao;
import br.com.jofashionhair.whatsapp.EvolutionApiClient;

/**
 * Envia a campanha em segundo plano, UMA mensagem a cada 3 segundos
 * (disparos em massa muito rápidos aumentam o risco de bloqueio do número).
 */
@Component
public class EnvioCampanhaWorker {

    private static final Logger log = LoggerFactory.getLogger(EnvioCampanhaWorker.class);
    private static final long INTERVALO_MS = 3_000;
    private static final String RODAPE = "\n\n_Para não receber mais promoções, responda *SAIR*._";

    private final CampanhaWhatsappRepository repository;
    private final EvolutionApiClient evolution;
    private final RelogioSalao relogio;

    public EnvioCampanhaWorker(CampanhaWhatsappRepository repository, EvolutionApiClient evolution, RelogioSalao relogio) {
        this.repository = repository;
        this.evolution = evolution;
        this.relogio = relogio;
    }

    @Async
    public void executar(Long campanhaId, List<Destinatario> destinatarios, String mensagem) {
        for (int i = 0; i < destinatarios.size(); i++) {
            Destinatario d = destinatarios.get(i);
            boolean ok;
            try {
                ok = evolution.enviarTexto(d.telefone(), personalizar(mensagem, d.nome()) + RODAPE) || !evolution.estaAtivo();
            } catch (RuntimeException e) {
                log.error("Falha ao enviar promoção para {}", d.telefone(), e);
                ok = false;
            }
            boolean sucesso = ok;
            repository.findById(campanhaId).ifPresent(c -> {
                c.registrar(sucesso);   // conta como enviada OU como falha, nunca as duas
                repository.save(c);
            });
            if (evolution.estaAtivo() && i < destinatarios.size() - 1) {
                try {
                    Thread.sleep(INTERVALO_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        repository.findById(campanhaId).ifPresent(c -> {
            c.concluir(relogio.agoraOffset());
            repository.save(c);
        });
    }

    /** {nome} → primeiro nome da cliente. */
    static String personalizar(String mensagem, String nome) {
        String primeiro = nome == null || nome.isBlank() || nome.startsWith("Cliente WhatsApp")
                ? "" : nome.trim().split("\\s+")[0];
        return mensagem.replace("{nome}", primeiro).replace(" ,", ",").replace("  ", " ");
    }
}
