package br.com.jofashionhair.demo;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import br.com.jofashionhair.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * POST /api/public/demo/iniciar  {"mestre": "chave opcional"}
 * <ul>
 *   <li>Com a chave mestra certa: modo DONO (edita os dados originais, com login).</li>
 *   <li>Sem ela: prepara a cópia particular do visitante e devolve o cookie.</li>
 * </ul>
 */
@RestController
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class DemoController {

    public record InicioRequest(String mestre) {
    }

    private final SessoesDemo sessoes;
    private final FiltroDemo filtro;
    private final boolean cookieSeguro;

    public DemoController(SessoesDemo sessoes, FiltroDemo filtro, AppProperties props) {
        this.sessoes = sessoes;
        this.filtro = filtro;
        this.cookieSeguro = props.seguranca() == null || props.seguranca().cookieSeguro();
    }

    @PostMapping("/api/public/demo/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar(@RequestBody(required = false) InicioRequest corpo,
                                                       HttpServletRequest req) {
        String mestre = corpo == null ? null : corpo.mestre();
        if (filtro.chaveMestraConfere(mestre)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie(FiltroDemo.COOKIE_MESTRE, filtro.assinaturaDono(), Duration.ofDays(30)))
                    .body(Map.of("modo", "BASE"));
        }
        if (filtro.ehDono(req)) {
            return ResponseEntity.ok(Map.of("modo", "BASE"));
        }

        Cookie atual = WebUtils.getCookie(req, FiltroDemo.COOKIE_SESSAO);
        String sessao = atual != null && atual.getValue().matches("^[0-9a-f-]{36}$")
                ? atual.getValue() : UUID.randomUUID().toString();
        try {
            sessoes.iniciar(sessao);
        } catch (DemoLotadaException e) {
            return ResponseEntity.status(503).body(Map.of("detail", e.getMessage(), "motivo", "DEMO_LOTADA"));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(FiltroDemo.COOKIE_SESSAO, sessao, Duration.ofHours(sessoes.horas())))
                .body(Map.of("modo", "VISITANTE", "horas", sessoes.horas()));
    }

    private String cookie(String nome, String valor, Duration validade) {
        return ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(cookieSeguro)
                .sameSite("Lax")
                .path("/")
                .maxAge(validade)
                .build()
                .toString();
    }
}
