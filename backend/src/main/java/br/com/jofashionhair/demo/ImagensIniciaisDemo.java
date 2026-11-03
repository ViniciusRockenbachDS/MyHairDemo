package br.com.jofashionhair.demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import br.com.jofashionhair.service.ArmazenamentoImagens;

/**
 * Deixa a demonstração bonita logo na primeira visita: grava as ilustrações de
 * resources/demo-imagens no banco e as liga aos serviços, produtos, equipe,
 * Lookbook e fotos do salão.
 *
 * Só preenche o que está SEM imagem: o que o dono trocar no modo dono não é
 * sobrescrito quando o servidor reinicia.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class ImagensIniciaisDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImagensIniciaisDemo.class);

    private static final Map<String, String> SERVICOS = new LinkedHashMap<>();
    private static final Map<String, String> PRODUTOS = new LinkedHashMap<>();
    private static final Map<String, String> EQUIPE = new LinkedHashMap<>();
    private static final List<String[]> LOOKBOOK = List.of(
            new String[] {"look-long-bob", "Corte long bob"},
            new String[] {"look-mechas", "Mechas iluminadas"},
            new String[] {"look-cachos", "Cachos definidos"},
            new String[] {"look-ruivo", "Ruivo acobreado"},
            new String[] {"look-escova-modelada", "Escova modelada"},
            new String[] {"look-loiro-perolado", "Loiro perolado"},
            new String[] {"look-unhas-nude", "Unhas nude"},
            new String[] {"look-make-festa", "Make para festa"});

    static {
        SERVICOS.put("Corte Feminino", "servico-corte-feminino");
        SERVICOS.put("Escova", "servico-escova");
        SERVICOS.put("Hidratação", "servico-hidratacao");
        SERVICOS.put("Coloração", "servico-coloracao");
        SERVICOS.put("Mechas", "servico-mechas");
        SERVICOS.put("Escova Progressiva", "servico-escova-progressiva");
        SERVICOS.put("Unha em Gel", "servico-unha-em-gel");
        SERVICOS.put("Manicure", "servico-manicure");
        SERVICOS.put("Pedicure", "servico-pedicure");
        SERVICOS.put("Manicure e Pedicure", "servico-manicure-e-pedicure");
        SERVICOS.put("Design de Sobrancelha", "servico-design-de-sobrancelha");
        SERVICOS.put("Maquiagem", "servico-maquiagem");
        SERVICOS.put("Limpeza de Pele", "servico-limpeza-de-pele");

        PRODUTOS.put("Shampoo Hidratante 300 ml", "produto-shampoo");
        PRODUTOS.put("Máscara de Reconstrução 250 g", "produto-mascara");
        PRODUTOS.put("Óleo Finalizador 60 ml", "produto-oleo");
        PRODUTOS.put("Kit Esmaltes Tendência", "produto-esmaltes");

        EQUIPE.put("Ana Paula", "profissional-ana-paula");
        EQUIPE.put("Bruna", "profissional-bruna");
        EQUIPE.put("Carla", "profissional-carla");
        EQUIPE.put("Daniela", "profissional-daniela");
    }

    private final JdbcTemplate jdbc;

    /** ArmazenamentoImagens garante que a tabela public.arquivos existe. */
    public ImagensIniciaisDemo(JdbcTemplate jdbc, @SuppressWarnings("unused") ArmazenamentoImagens armazenamento) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // salão fictício (a demonstração não usa o nome de um cliente real)
        jdbc.update("""
                UPDATE public.configuracao_salao
                   SET nome = 'Studio Aurora', cidade = 'Florianópolis - SC', instagram = NULL,
                       slogan = COALESCE(slogan, 'Beleza com hora marcada')
                 WHERE id = 1 AND nome = 'Jo Fashion Hair'""");

        jdbc.update("UPDATE public.configuracao_salao SET foto_hero_url = ? WHERE id = 1 AND foto_hero_url IS NULL",
                url("salao-fundo-principal"));
        jdbc.update("UPDATE public.configuracao_salao SET foto_espaco_url = ? WHERE id = 1 AND foto_espaco_url IS NULL",
                url("salao-espaco"));
        jdbc.update("UPDATE public.configuracao_salao SET foto_visita_url = ? WHERE id = 1 AND foto_visita_url IS NULL",
                url("salao-fachada"));

        SERVICOS.forEach((nome, arq) -> jdbc.update(
                "UPDATE public.servicos SET foto_url = ? WHERE nome = ? AND foto_url IS NULL", url(arq), nome));
        PRODUTOS.forEach((nome, arq) -> jdbc.update(
                "UPDATE public.produtos SET foto_url = ? WHERE nome = ? AND foto_url IS NULL", url(arq), nome));
        EQUIPE.forEach((nome, arq) -> jdbc.update(
                "UPDATE public.profissionais SET foto_url = ? WHERE nome = ? AND foto_url IS NULL", url(arq), nome));

        Integer fotos = jdbc.queryForObject("SELECT COUNT(*) FROM public.lookbook_fotos", Integer.class);
        if (fotos != null && fotos == 0) {
            for (int i = 0; i < LOOKBOOK.size(); i++) {
                jdbc.update("INSERT INTO public.lookbook_fotos (foto_url, legenda, ordem) VALUES (?, ?, ?)",
                        url(LOOKBOOK.get(i)[0]), LOOKBOOK.get(i)[1], i + 1);
            }
        }
        log.info("Demonstração: ilustrações de exemplo prontas");
    }

    /** Grava a imagem no banco (uma vez só) e devolve o endereço público dela. */
    private String url(String arquivo) {
        String nome = UUID.nameUUIDFromBytes(("myhair-demo/" + arquivo).getBytes(StandardCharsets.UTF_8)) + ".jpg";
        Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM public.arquivos WHERE nome = ?", Integer.class, nome);
        if (existe == null || existe == 0) {
            try (InputStream in = new ClassPathResource("demo-imagens/" + arquivo + ".jpg").getInputStream()) {
                jdbc.update("INSERT INTO public.arquivos (nome, dados, origem) VALUES (?, ?, 'BASE') ON CONFLICT DO NOTHING",
                        nome, in.readAllBytes());
            } catch (IOException e) {
                log.warn("Imagem de exemplo não encontrada: {}", arquivo);
            }
        }
        return ArmazenamentoImagens.PREFIXO_URL + nome;
    }
}
