package br.com.jofashionhair.web.admin;

import java.util.List;

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

import br.com.jofashionhair.service.AvaliacaoDto;
import br.com.jofashionhair.service.AvaliacaoService;
import br.com.jofashionhair.service.ResumoAvaliacoesDto;
import jakarta.validation.Valid;

/**
 * /api/admin/avaliacoes
 * <pre>
 * GET    /          todas (visíveis e ocultas)
 * POST   /          nova
 * PUT    /{id}      editar (inclusive ocultar: "ativo": false)
 * DELETE /{id}      excluir
 * GET    /resumo    nota geral, total e link "Ver todas"
 * PUT    /resumo
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/avaliacoes")
public class AvaliacaoAdminController {

    private final AvaliacaoService service;

    public AvaliacaoAdminController(AvaliacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<AvaliacaoDto> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoDto criar(@Valid @RequestBody AvaliacaoDto dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public AvaliacaoDto atualizar(@PathVariable Long id, @Valid @RequestBody AvaliacaoDto dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }

    @GetMapping("/resumo")
    public ResumoAvaliacoesDto resumo() {
        return service.resumo();
    }

    @PutMapping("/resumo")
    public ResumoAvaliacoesDto salvarResumo(@Valid @RequestBody ResumoAvaliacoesDto dto) {
        return service.salvarResumo(dto);
    }
}
