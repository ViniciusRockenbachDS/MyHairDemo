package br.com.jofashionhair.conta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Sem login (tela de entrar do painel):
 * <pre>
 * POST /api/public/conta/esqueci-senha     {"email"}                       → envia código
 * POST /api/public/conta/redefinir-senha   {"email","codigo","novaSenha"}  → nova senha (também 1º acesso)
 * </pre>
 */
@RestController
@RequestMapping("/api/public/conta")
public class ContaPublicaController {

    private final ContaService service;

    public ContaPublicaController(ContaService service) {
        this.service = service;
    }

    @PostMapping("/esqueci-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ContaDtos.Mensagem esqueciSenha(@Valid @RequestBody ContaDtos.EsqueciSenhaRequest req) {
        return new ContaDtos.Mensagem(service.esqueciSenha(req.email()));
    }

    @PostMapping("/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinirSenha(@Valid @RequestBody ContaDtos.RedefinirSenhaRequest req) {
        service.redefinirSenha(req.email(), req.codigo(), req.novaSenha());
    }
}
