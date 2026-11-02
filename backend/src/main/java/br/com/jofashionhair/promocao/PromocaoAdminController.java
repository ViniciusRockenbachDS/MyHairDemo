package br.com.jofashionhair.promocao;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.promocao.PromocaoDtos.CampanhaResponse;
import br.com.jofashionhair.promocao.PromocaoDtos.EnvioRequest;
import br.com.jofashionhair.promocao.PromocaoDtos.PromocaoDto;
import br.com.jofashionhair.promocao.PromocaoDtos.PublicoRequest;
import br.com.jofashionhair.promocao.PromocaoDtos.PublicoResponse;
import br.com.jofashionhair.service.admin.AdminDtos.AtivoRequest;
import jakarta.validation.Valid;

/**
 * <pre>
 * GET    /api/admin/promocoes
 * POST   /api/admin/promocoes
 * PUT    /api/admin/promocoes/{id}
 * PATCH  /api/admin/promocoes/{id}/ativo
 * DELETE /api/admin/promocoes/{id}
 * POST   /api/admin/promocoes/{id}/publico   quantas clientes receberiam
 * POST   /api/admin/promocoes/{id}/enviar    envia por WhatsApp (só o DONO)
 * GET    /api/admin/campanhas                histórico de envios
 * </pre>
 */
@RestController
public class PromocaoAdminController {

    private final PromocaoService promocaoService;
    private final CampanhaService campanhaService;

    public PromocaoAdminController(PromocaoService promocaoService, CampanhaService campanhaService) {
        this.promocaoService = promocaoService;
        this.campanhaService = campanhaService;
    }

    @GetMapping("/api/admin/promocoes")
    public List<PromocaoDto> listar() {
        return promocaoService.listar();
    }

    @PostMapping("/api/admin/promocoes")
    @ResponseStatus(HttpStatus.CREATED)
    public PromocaoDto criar(@Valid @RequestBody PromocaoDto dto) {
        return promocaoService.criar(dto);
    }

    @PutMapping("/api/admin/promocoes/{id}")
    public PromocaoDto atualizar(@PathVariable Long id, @Valid @RequestBody PromocaoDto dto) {
        return promocaoService.atualizar(id, dto);
    }

    @PatchMapping("/api/admin/promocoes/{id}/ativo")
    public PromocaoDto alterarAtivo(@PathVariable Long id, @Valid @RequestBody AtivoRequest req) {
        return promocaoService.alterarAtivo(id, req.ativo());
    }

    @DeleteMapping("/api/admin/promocoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        promocaoService.remover(id);
    }

    @PostMapping("/api/admin/promocoes/{id}/publico")
    public PublicoResponse publico(@PathVariable Long id, @RequestBody PublicoRequest req) {
        return new PublicoResponse(campanhaService.contarPublico(id,
                req.soAutorizadas() == null || req.soAutorizadas(), Boolean.TRUE.equals(req.soQuemFezServicos())));
    }

    @PostMapping("/api/admin/promocoes/{id}/enviar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CampanhaResponse enviar(@PathVariable Long id, @Valid @RequestBody EnvioRequest req) {
        return campanhaService.enviar(id, req);
    }

    @GetMapping("/api/admin/campanhas")
    public List<CampanhaResponse> campanhas() {
        return campanhaService.historico();
    }
}
