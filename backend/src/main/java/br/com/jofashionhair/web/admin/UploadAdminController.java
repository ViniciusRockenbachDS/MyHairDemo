package br.com.jofashionhair.web.admin;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.jofashionhair.service.ArmazenamentoImagens;

/** POST /api/admin/uploads (campo "arquivo") → {"url": "/api/public/arquivos/...jpg"} */
@RestController
public class UploadAdminController {

    private final ArmazenamentoImagens armazenamento;

    public UploadAdminController(ArmazenamentoImagens armazenamento) {
        this.armazenamento = armazenamento;
    }

    @PostMapping(path = "/api/admin/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> enviar(@RequestParam("arquivo") MultipartFile arquivo) {
        return Map.of("url", armazenamento.salvarImagem(arquivo));
    }
}
