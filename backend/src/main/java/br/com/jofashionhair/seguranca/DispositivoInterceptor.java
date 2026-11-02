package br.com.jofashionhair.seguranca;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

import br.com.jofashionhair.conta.DispositivoService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Depois do e-mail e senha (Spring Security), exige que o navegador seja um
 * dispositivo confirmado. Se não for, responde 403 com
 * motivo = DISPOSITIVO_NAO_VERIFICADO e o painel pede o código do e-mail.
 * As rotas /api/admin/dispositivo/** ficam liberadas (são as que confirmam).
 */
@Component
public class DispositivoInterceptor implements HandlerInterceptor {

    private final DispositivoService dispositivoService;

    public DispositivoInterceptor(DispositivoService dispositivoService) {
        this.dispositivoService = dispositivoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(req.getMethod()) || br.com.jofashionhair.demo.ContextoDemo.ehVisitante()) {
            return true;   // visitante da demonstração não confirma dispositivo
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;   // sem login o Spring Security já barrou
        }
        Cookie cookie = WebUtils.getCookie(req, DispositivoService.COOKIE);
        if (dispositivoService.confiavel(auth.getName(), cookie == null ? null : cookie.getValue())) {
            return true;
        }
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/problem+json;charset=UTF-8");
        res.getWriter().write("{\"status\":403,\"title\":\"Dispositivo não confirmado\","
                + "\"detail\":\"Confirme este dispositivo com o código enviado ao seu e-mail.\","
                + "\"motivo\":\"DISPOSITIVO_NAO_VERIFICADO\"}");
        return false;
    }
}
