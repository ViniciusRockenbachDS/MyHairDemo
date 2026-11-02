package br.com.jofashionhair.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Segurança da API.
 *
 * <ul>
 *   <li>/api/public/** e /api/webhooks/** → abertos (o webhook valida um segredo próprio no Passo 4)</li>
 *   <li>/api/admin/** → exige login (e-mail e senha de um usuário ativo)</li>
 *   <li>/api/admin/usuarios/** → só o DONO (gerenciar acessos da equipe)</li>
 *   <li>qualquer outra rota → negada</li>
 * </ul>
 *
 * <p>HTTP Basic é simples e seguro <b>desde que a API rode atrás de HTTPS</b>.
 * O 401 é devolvido SEM o cabeçalho WWW-Authenticate para o navegador não
 * abrir a janelinha nativa de login: o React mostra a própria tela.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, br.com.jofashionhair.config.AppProperties props)
            throws Exception {
        AuthenticationEntryPoint naoAutenticado =
                (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED);

        http
                .csrf(AbstractHttpConfigurer::disable)          // API stateless, sem cookie de sessão
                .cors(Customizer.withDefaults())                // usa o CorsConfig do Spring MVC
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error", "/actuator/health").permitAll()
                        // páginas e arquivos do site (servidos pelo próprio servidor na demonstração)
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/assets/**", "/agendar", "/privacidade",
                                "/admin", "/admin/**", "/*.svg", "/*.png", "/*.ico", "/*.webmanifest").permitAll()
                        .requestMatchers("/api/public/**", "/api/webhooks/**").permitAll()
                        .requestMatchers("/api/admin/usuarios/**").hasRole("DONO")
                        .requestMatchers(HttpMethod.POST, "/api/admin/promocoes/*/enviar").hasRole("DONO")
                        .requestMatchers("/api/admin/**").hasAnyRole("DONO", "EQUIPE")
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(naoAutenticado))
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutenticado));

        if (props.demo() != null && props.demo().habilitado()) {
            // visitante da demonstração: painel sem senha (só na cópia dele)
            http.addFilterBefore(new br.com.jofashionhair.demo.AutenticacaoVisitante(),
                    org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
