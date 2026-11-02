package br.com.jofashionhair.conta;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Painel (com login):
 * <pre>
 * GET   /api/admin/me                       quem está logado
 * POST  /api/admin/conta/senha/codigo       envia código para o e-mail da conta
 * PUT   /api/admin/conta/senha              {"codigo","novaSenha"}
 *
 * Só DONO:
 * GET   /api/admin/usuarios                 acessos da equipe
 * POST  /api/admin/usuarios                 {"nome","email","papel"}  convida
 * POST  /api/admin/usuarios/{id}/convite    reenvia o convite
 * PATCH /api/admin/usuarios/{id}/ativo      {"ativo": false}
 * </pre>
 */
@RestController
public class ContaAdminController {

    private final ContaService service;

    public ContaAdminController(ContaService service) {
        this.service = service;
    }

    @GetMapping("/api/admin/me")
    public ContaDtos.UsuarioResponse me(Principal principal) {
        return service.minhaConta(principal.getName());
    }

    @PostMapping("/api/admin/conta/senha/codigo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ContaDtos.Mensagem pedirCodigo(Principal principal) {
        return new ContaDtos.Mensagem(service.solicitarCodigoTroca(principal.getName()));
    }

    @PutMapping("/api/admin/conta/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(Principal principal, @Valid @RequestBody ContaDtos.TrocarSenhaRequest req,
                            @CookieValue(name = DispositivoService.COOKIE, required = false) String tokenDispositivo) {
        service.trocarSenha(principal.getName(), req.codigo(), req.novaSenha(), tokenDispositivo);
    }

    @GetMapping("/api/admin/usuarios")
    public List<ContaDtos.UsuarioResponse> usuarios() {
        return service.listarUsuarios();
    }

    @PostMapping("/api/admin/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ContaDtos.UsuarioResponse convidar(@Valid @RequestBody ContaDtos.ConviteRequest req) {
        return service.convidar(req);
    }

    @PostMapping("/api/admin/usuarios/{id}/convite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarConvite(@PathVariable Long id) {
        service.reenviarConvite(id);
    }

    @PatchMapping("/api/admin/usuarios/{id}/ativo")
    public ContaDtos.UsuarioResponse alterarAtivo(@PathVariable Long id, Principal principal,
                                                  @Valid @RequestBody ContaDtos.AtivoRequest req) {
        return service.alterarAtivo(id, req.ativo(), principal.getName());
    }
}
