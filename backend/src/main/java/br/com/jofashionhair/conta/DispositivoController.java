package br.com.jofashionhair.conta;

import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * <pre>
 * Liberadas sem dispositivo confirmado (mas exigem e-mail e senha):
 * POST   /api/admin/dispositivo/enviar-codigo
 * POST   /api/admin/dispositivo/confirmar         {"codigo"}  → grava o cookie
 *
 * Dispositivos da própria conta:
 * GET    /api/admin/dispositivos
 * DELETE /api/admin/dispositivos/{id}
 * POST   /api/admin/dispositivos/sair-dos-outros
 * </pre>
 */
@RestController
public class DispositivoController {

    public record ConfirmarRequest(
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "O código tem 6 números.") String codigo) {
    }

    private final DispositivoService service;
    private final boolean cookieSeguro;

    public DispositivoController(DispositivoService service, AppProperties props) {
        this.service = service;
        this.cookieSeguro = props.seguranca() == null || props.seguranca().cookieSeguro();
    }

    @PostMapping("/api/admin/dispositivo/enviar-codigo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ContaDtos.Mensagem enviarCodigo(Principal principal,
                                           @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return new ContaDtos.Mensagem(service.enviarCodigo(principal.getName(), userAgent));
    }

    @PostMapping("/api/admin/dispositivo/confirmar")
    public ResponseEntity<Void> confirmar(Principal principal, @Valid @RequestBody ConfirmarRequest req,
                                          @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent,
                                          HttpServletRequest http) {
        String token = service.confirmar(principal.getName(), req.codigo(), userAgent, http.getRemoteAddr());
        ResponseCookie cookie = ResponseCookie.from(DispositivoService.COOKIE, token)
                .httpOnly(true)                 // JavaScript não lê
                .secure(cookieSeguro)           // só HTTPS em produção
                .sameSite("Strict")
                .path("/api/admin")
                .maxAge(Duration.ofDays(service.diasValidade()))
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @GetMapping("/api/admin/dispositivos")
    public List<DispositivoService.DispositivoResponse> listar(Principal principal,
            @CookieValue(name = DispositivoService.COOKIE, required = false) String token) {
        return service.listar(principal.getName(), token);
    }

    @DeleteMapping("/api/admin/dispositivos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(Principal principal, @PathVariable Long id) {
        service.remover(principal.getName(), id);
    }

    @PostMapping("/api/admin/dispositivos/sair-dos-outros")
    public Map<String, Integer> sairDosOutros(Principal principal,
            @CookieValue(name = DispositivoService.COOKIE, required = false) String token) {
        return Map.of("removidos", service.sairDosOutros(principal.getName(), token));
    }
}
