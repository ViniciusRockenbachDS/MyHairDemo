package br.com.jofashionhair.config;

import org.springframework.context.annotation.Configuration;

import br.com.jofashionhair.seguranca.DispositivoInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Proíbe cache das respostas da API pública (navegador, CDN ou proxy).
 * Sem isso, um preço alterado no /admin poderia continuar aparecendo antigo
 * no site por causa de uma cópia guardada no caminho.
 */
@Configuration
public class CacheControlConfig implements WebMvcConfigurer {

    private final DispositivoInterceptor dispositivoInterceptor;

    public CacheControlConfig(DispositivoInterceptor dispositivoInterceptor) {
        this.dispositivoInterceptor = dispositivoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store, max-age=0");
                return true;
            }
        }).addPathPatterns("/api/public/**")
                // imagens têm nome único: podem ficar em cache (ver ArquivoPublicoController)
                .excludePathPatterns("/api/public/arquivos/**");

        // painel: além de e-mail e senha, exige dispositivo confirmado por código no e-mail
        registry.addInterceptor(dispositivoInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/dispositivo/**");
    }
}
