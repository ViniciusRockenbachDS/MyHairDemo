package br.com.jofashionhair.demo;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Visitante da demonstração entra no painel sem senha, como DONO da própria
 * cópia. Não vale para os dados originais (esses exigem o login normal).
 */
public class AutenticacaoVisitante extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (ContextoDemo.ehVisitante() && req.getRequestURI().startsWith("/api/admin/")) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    SessoesDemo.EMAIL_VISITANTE, null, List.of(new SimpleGrantedAuthority("ROLE_DONO"))));
        }
        chain.doFilter(req, res);
    }
}
