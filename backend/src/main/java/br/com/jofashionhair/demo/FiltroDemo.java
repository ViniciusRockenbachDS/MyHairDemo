package br.com.jofashionhair.demo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import br.com.jofashionhair.config.AppProperties;
import br.com.jofashionhair.seguranca.LimitadorTaxa;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Antes de tudo: decide em qual cópia do banco a requisição trabalha.
 *
 * <ul>
 *   <li>Cookie do dono (chave mestra) → dados originais (com login normal).</li>
 *   <li>Cookie de visitante → a cópia particular dele.</li>
 *   <li>Sem cookie: só leitura dos dados originais (GET em /api/public).
 *       Qualquer alteração é recusada com "DEMO_EXPIRADA" e o site recarrega.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class FiltroDemo extends OncePerRequestFilter {

    public static final String COOKIE_SESSAO = "demo_sessao";
    public static final String COOKIE_MESTRE = "demo_mestre";
    private static final int MAX_IMAGENS_POR_VISITA = 30;

    private final SessoesDemo sessoes;
    private final LimitadorTaxa limitador;
    private final String chaveMestra;

    public FiltroDemo(SessoesDemo sessoes, LimitadorTaxa limitador, AppProperties props) {
        this.sessoes = sessoes;
        this.limitador = limitador;
        this.chaveMestra = props.demo().chaveMestra();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String caminho = req.getRequestURI();
        if (!caminho.startsWith("/api/")) {
            chain.doFilter(req, res);   // páginas e arquivos do site
            return;
        }
        try {
            if (ehDono(req)) {
                ContextoDemo.definir("public", ContextoDemo.Modo.BASE, null);
            } else {
                Cookie cookie = WebUtils.getCookie(req, COOKIE_SESSAO);
                String sessao = cookie == null ? null : cookie.getValue();
                Optional<String> schema = sessao == null || !sessao.matches("^[0-9a-f-]{36}$")
                        ? Optional.empty() : sessoes.esquemaDaSessao(sessao);
                if (schema.isPresent()) {
                    ContextoDemo.definir(schema.get(), ContextoDemo.Modo.VISITANTE, sessao);
                    if ("POST".equals(req.getMethod()) && caminho.equals("/api/admin/uploads")
                            && limitador.registrar("demo-upload:" + sessao, Duration.ofHours(sessoes.horas()))
                            > MAX_IMAGENS_POR_VISITA) {
                        responder(res, 429, "Limite de imagens desta demonstração atingido.", null);
                        return;
                    }
                } else if (!caminho.startsWith("/api/public/demo/")
                        && (!"GET".equals(req.getMethod()) || caminho.startsWith("/api/admin/"))) {
                    responder(res, 409, "Sua demonstração expirou. A página vai recarregar para começar outra.",
                            "DEMO_EXPIRADA");
                    return;
                }
            }
            chain.doFilter(req, res);
        } finally {
            ContextoDemo.limpar();
        }
    }

    /** O cookie do dono é uma assinatura da chave mestra (não a chave em si). */
    public boolean ehDono(HttpServletRequest req) {
        Cookie cookie = WebUtils.getCookie(req, COOKIE_MESTRE);
        String esperado = assinaturaDono();
        return cookie != null && esperado != null
                && MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                        cookie.getValue().getBytes(StandardCharsets.UTF_8));
    }

    public boolean chaveMestraConfere(String chave) {
        return chaveMestra != null && !chaveMestra.isBlank() && chave != null
                && MessageDigest.isEqual(chaveMestra.getBytes(StandardCharsets.UTF_8), chave.getBytes(StandardCharsets.UTF_8));
    }

    public String assinaturaDono() {
        if (chaveMestra == null || chaveMestra.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveMestra.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            StringBuilder hex = new StringBuilder();
            for (byte b : mac.doFinal("dono-da-demonstracao".getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static void responder(HttpServletResponse res, int status, String mensagem, String motivo) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + mensagem + "\""
                + (motivo != null ? ",\"motivo\":\"" + motivo + "\"" : "") + "}");
    }
}
