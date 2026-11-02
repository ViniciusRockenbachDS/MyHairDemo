package br.com.jofashionhair.web.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.service.admin.AdminDtos.BloqueioRequest;
import br.com.jofashionhair.service.admin.AdminDtos.BloqueioResponse;
import br.com.jofashionhair.service.admin.BloqueioAdminService;
import jakarta.validation.Valid;

/**
 * /api/admin/bloqueios
 * <pre>
 * GET    /?de=2026-10-01&ate=2026-10-31[&profissionalId=1]
 * POST   /   {"profissionalId":1,"inicio":"2026-10-17T08:00","fim":"2026-10-17T12:00","motivo":"Curso"}
 * DELETE /{id}
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/bloqueios")
public class BloqueioAdminController {

    private final BloqueioAdminService service;

    public BloqueioAdminController(BloqueioAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<BloqueioResponse> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long profissionalId) {
        return service.listar(de, ate, profissionalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioResponse criar(@Valid @RequestBody BloqueioRequest req) {
        return service.criar(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
