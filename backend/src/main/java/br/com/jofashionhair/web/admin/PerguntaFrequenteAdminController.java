package br.com.jofashionhair.web.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.service.PerguntaFrequenteDto;
import br.com.jofashionhair.whatsapp.RespostasFrequentes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * /api/admin/perguntas
 * <pre>
 * GET    /           todas
 * POST   /           nova
 * PUT    /{id}       editar (inclusive desativar: "ativo": false)
 * DELETE /{id}       excluir
 * POST   /testar     {"texto": "vocês aceitam pix?"} → qual resposta o robô daria
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/perguntas")
public class PerguntaFrequenteAdminController {

    public record TesteRequest(@NotBlank String texto) {
    }

    private final RespostasFrequentes service;

    public PerguntaFrequenteAdminController(RespostasFrequentes service) {
        this.service = service;
    }

    @GetMapping
    public List<PerguntaFrequenteDto> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerguntaFrequenteDto criar(@Valid @RequestBody PerguntaFrequenteDto dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public PerguntaFrequenteDto atualizar(@PathVariable Long id, @Valid @RequestBody PerguntaFrequenteDto dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    @PostMapping("/testar")
    public Map<String, Object> testar(@Valid @RequestBody TesteRequest req) {
        Map<String, Object> r = new HashMap<>();
        service.encontrar(req.texto()).ifPresentOrElse(p -> {
            r.put("encontrou", true);
            r.put("pergunta", p.getPergunta());
            r.put("resposta", p.getResposta());
        }, () -> r.put("encontrou", false));
        return r;
    }
}
