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

import br.com.jofashionhair.service.FotoLookbookDto;
import br.com.jofashionhair.service.LookbookService;
import jakarta.validation.Valid;

/** /api/admin/lookbook — GET, POST, PUT /{id}, DELETE /{id} */
@RestController
@RequestMapping("/api/admin/lookbook")
public class LookbookAdminController {

    private final LookbookService service;

    public LookbookAdminController(LookbookService service) {
        this.service = service;
    }

    @GetMapping
    public List<FotoLookbookDto> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FotoLookbookDto criar(@Valid @RequestBody FotoLookbookDto dto) {
        return service.criar(dto);
    }

    @PutMapping("/{id}")
    public FotoLookbookDto atualizar(@PathVariable Long id, @Valid @RequestBody FotoLookbookDto dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
