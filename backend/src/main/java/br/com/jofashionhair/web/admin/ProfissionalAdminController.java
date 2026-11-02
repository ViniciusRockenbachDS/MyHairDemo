package br.com.jofashionhair.web.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
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
import br.com.jofashionhair.service.admin.AdminDtos.HorariosRequest;
import br.com.jofashionhair.service.admin.AdminDtos.ProfissionalAdminResponse;
import br.com.jofashionhair.service.admin.AdminDtos.ProfissionalRequest;
import br.com.jofashionhair.service.admin.AdminDtos.ServicosDaProfissionalRequest;
import br.com.jofashionhair.service.admin.ProfissionalAdminService;
import jakarta.validation.Valid;

/**
 * /api/admin/profissionais
 * <pre>
 * GET   /                 lista (com ids dos serviços)
 * GET   /{id}             detalhe (com grade de horários)
 * POST  /                 cria
 * PUT   /{id}             atualiza dados
 * PATCH /{id}/ativo       ativa/desativa
 * PUT   /{id}/servicos    {"servicoIds":[1,2,3]}  substitui os serviços vinculados
 * PUT   /{id}/horarios    {"faixas":[{"diaSemana":1,"horaInicio":"09:00","horaFim":"12:00"}]}
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/profissionais")
public class ProfissionalAdminController {

    private final ProfissionalAdminService service;

    public ProfissionalAdminController(ProfissionalAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProfissionalAdminResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ProfissionalAdminResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfissionalAdminResponse criar(@Valid @RequestBody ProfissionalRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public ProfissionalAdminResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProfissionalRequest req) {
        return service.atualizar(id, req);
    }

    @PatchMapping("/{id}/ativo")
    public ProfissionalAdminResponse alterarAtivo(@PathVariable Long id, @Valid @RequestBody AtivoRequest req) {
        return service.alterarAtivo(id, req.ativo());
    }

    @PutMapping("/{id}/servicos")
    public ProfissionalAdminResponse definirServicos(@PathVariable Long id,
                                                     @Valid @RequestBody ServicosDaProfissionalRequest req) {
        return service.definirServicos(id, req.servicoIds());
    }

    @PutMapping("/{id}/horarios")
    public ProfissionalAdminResponse definirHorarios(@PathVariable Long id,
                                                     @Valid @RequestBody HorariosRequest req) {
        return service.definirHorarios(id, req.faixas());
    }
}
