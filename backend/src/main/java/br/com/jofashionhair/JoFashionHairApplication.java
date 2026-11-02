package br.com.jofashionhair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ponto de entrada da API do Jo Fashion Hair.
 *
 * <p>{@link EnableScheduling} habilita a rotina de lembretes automáticos (Passo 5).
 * {@link ConfigurationPropertiesScan} registra o AppProperties (prefixo "app").</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan
public class JoFashionHairApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoFashionHairApplication.class, args);
    }
}
