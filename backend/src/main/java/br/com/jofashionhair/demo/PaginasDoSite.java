package br.com.jofashionhair.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Site e API no mesmo endereço (necessário para o cookie da demonstração):
 * as páginas do React são entregues pelo próprio servidor.
 */
@Controller
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class PaginasDoSite {

    @GetMapping({"/agendar", "/privacidade", "/admin", "/admin/{*resto}"})
    public String paginaDoSite() {
        return "forward:/index.html";
    }
}
