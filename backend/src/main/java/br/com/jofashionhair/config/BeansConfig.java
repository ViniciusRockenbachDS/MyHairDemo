package br.com.jofashionhair.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    /**
     * Relógio no fuso do salão. Injetar {@link Clock} (em vez de chamar
     * LocalDateTime.now() direto) permite testar regras de horário com datas fixas.
     */
    @Bean
    public Clock clock(AppProperties props) {
        return Clock.system(props.zoneId());
    }
}
