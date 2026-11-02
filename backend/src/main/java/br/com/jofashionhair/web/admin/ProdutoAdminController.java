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

import br.com.jofashionhair.service.ProdutoDto;
import br.com.jofashionhair.service.ProdutoService;
import jakarta.validation.Valid;

/**
 * /api/admin/produtos
 * <pre>
 * GET    /        todos (visíveis e ocultos)
 * POST   /        novo
 * PUT    /{id}    editar (inclusive esgotado: "disponivel": false, ou ocultar: "ativo": false)
 * DELETE /{id}    excluir
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/produtos")
public class ProdutoAdminController {

    private final ProdutoService service;

    public ProdutoAdminController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProdutoDto> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoDto criar(@Valid @RequestBody ProdutoDto dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public ProdutoDto atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoDto dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
