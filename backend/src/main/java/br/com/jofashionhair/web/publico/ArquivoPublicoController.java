package br.com.jofashionhair.web.publico;

import java.time.Duration;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.service.ArmazenamentoImagens;

/** GET /api/public/arquivos/{nome} → entrega as imagens enviadas pelo painel. */
@RestController
public class ArquivoPublicoController {

    private final ArmazenamentoImagens armazenamento;

    public ArquivoPublicoController(ArmazenamentoImagens armazenamento) {
        this.armazenamento = armazenamento;
    }

    @GetMapping("/api/public/arquivos/{nome}")
    public ResponseEntity<Resource> arquivo(@PathVariable String nome) {
        return armazenamento.carregar(nome)
                .<ResponseEntity<Resource>>map(dados -> ResponseEntity.ok()
                        .contentType(tipo(nome))
                        // nome é único: trocar a foto gera outro nome, então pode ficar em cache
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .body(new ByteArrayResource(dados)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static MediaType tipo(String nome) {
        if (nome.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (nome.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
