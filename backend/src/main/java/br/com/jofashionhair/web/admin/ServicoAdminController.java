package br.com.jofashionhair.web.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.service.admin.AdminDtos.AtivoRequest;
import br.com.jofashionhair.service.admin.AdminDtos.PrecoDuracaoRequest;
import br.com.jofashionhair.service.admin.AdminDtos.ServicoAdminResponse;
import br.com.jofashionhair.service.admin.AdminDtos.ServicoRequest;
import br.com.jofashionhair.service.admin.ServicoAdminService;
import jakarta.validation.Valid;

/**
 * /api/admin/servicos
 * <pre>
 * GET    /                      lista todos (ativos e inativos)
 * POST   /                      cria
 * PUT    /{id}                  atualiza tudo
 * PATCH  /{id}/preco-duracao    edição rápida de preço e/ou duração
 * PATCH  /{id}/ativo            ativa/desativa  {"ativo": true}
 * DELETE /{id}                  "remove" (desativa, preservando o histórico)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/servicos")
public class ServicoAdminController {

    private final ServicoAdminService service;

    public ServicoAdminController(ServicoAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServicoAdminResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoAdminResponse criar(@Valid @RequestBody ServicoRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public ServicoAdminResponse atualizar(@PathVariable Long id, @Valid @RequestBody ServicoRequest req) {
        return service.atualizar(id, req);
    }

    @PatchMapping("/{id}/preco-duracao")
    public ServicoAdminResponse atualizarPrecoDuracao(@PathVariable Long id,
                                                      @Valid @RequestBody PrecoDuracaoRequest req) {
        return service.atualizarPrecoDuracao(id, req);
    }

    @PatchMapping("/{id}/ativo")
    public ServicoAdminResponse alterarAtivo(@PathVariable Long id, @Valid @RequestBody AtivoRequest req) {
        return service.alterarAtivo(id, req.ativo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.alterarAtivo(id, false);
    }
}
