package br.com.jofashionhair.seguranca;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.jofashionhair.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite de tentativas por endereço IP (proteção contra força bruta e abuso).
 *
 * <ul>
 *   <li><b>Login do painel:</b> 10 senhas erradas em 15 min → IP bloqueado por 15 min.</li>
 *   <li><b>Agendamento pelo site:</b> no máximo 10 agendamentos por hora por IP.</li>
 *   <li><b>Rotas públicas:</b> no máximo 300 requisições por minuto por IP.</li>
 * </ul>
 *
 * <p>Roda ANTES do Spring Security, para conseguir contar as respostas 401
 * (senha errada). Atrás do Nginx, o IP real vem do cabeçalho X-Forwarded-For
 * (ver server.forward-headers-strategy no application.yml).</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LimiteTentativasFilter extends OncePerRequestFilter {

    private static final int MAX_FALHAS_LOGIN = 10;
    private static final Duration JANELA_LOGIN = Duration.ofMinutes(15);
    private static final int MAX_AGENDAMENTOS = 10;
    private static final Duration JANELA_AGENDAMENTOS = Duration.ofHours(1);
    private static final int MAX_PUBLICAS = 300;
    private static final Duration JANELA_PUBLICAS = Duration.ofMinutes(1);

    private final LimitadorTaxa limitador;
    private final List<String> origensPermitidas;

    public LimiteTentativasFilter(LimitadorTaxa limitador, AppProperties props) {
        this.limitador = limitador;
        this.origensPermitidas = props.cors() == null || props.cors().allowedOrigins() == null
                ? List.of() : props.cors().allowedOrigins();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String caminho = req.getRequestURI();
        String ip = req.getRemoteAddr();

        if (caminho.startsWith("/api/admin/")) {
            String chave = "login:" + ip;
            if (limitador.contagem(chave) >= MAX_FALHAS_LOGIN) {
                bloquear(req, res, "Muitas tentativas de login com senha errada. Tente novamente em "
                        + minutos(limitador.segundosRestantes(chave)) + ".");
                return;
            }
            chain.doFilter(req, res);
            if (res.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
                limitador.registrar(chave, JANELA_LOGIN);
            }
            return;
        }

        if (caminho.startsWith("/api/public/") && !caminho.startsWith("/api/public/eventos")) {
            if (HttpMethod.POST.matches(req.getMethod()) && caminho.equals("/api/public/agendamentos")) {
                String chave = "agendar:" + ip;
                if (limitador.registrar(chave, JANELA_AGENDAMENTOS) > MAX_AGENDAMENTOS) {
                    bloquear(req, res, "Muitos agendamentos seguidos deste aparelho. Tente novamente em "
                            + minutos(limitador.segundosRestantes(chave)) + " ou fale com o salão pelo WhatsApp.");
                    return;
                }
            }
            String chave = "publico:" + ip;
            if (limitador.registrar(chave, JANELA_PUBLICAS) > MAX_PUBLICAS) {
                bloquear(req, res, "Muitas requisições em pouco tempo. Aguarde um minuto e tente de novo.");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private void bloquear(HttpServletRequest req, HttpServletResponse res, String mensagem) throws IOException {
        // sem os cabeçalhos de CORS o navegador esconderia a mensagem e mostraria "sem conexão"
        String origem = req.getHeader("Origin");
        if (origem != null && origensPermitidas.contains(origem)) {
            res.setHeader("Access-Control-Allow-Origin", origem);
            res.setHeader("Vary", "Origin");
        }
        res.setStatus(429);
        res.setHeader("Retry-After", "60");
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"status\":429,\"title\":\"Muitas tentativas\",\"detail\":\""
                + mensagem.replace("\"", "'") + "\"}");
    }

    private static String minutos(long segundos) {
        long m = Math.max(1, (segundos + 59) / 60);
        return m == 1 ? "1 minuto" : m + " minutos";
    }
}
