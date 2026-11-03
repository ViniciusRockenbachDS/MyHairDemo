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

    /** autor, texto, dias atrás, imagem */
    private static final List<String[]> AVALIACOES = List.of(
            new String[] {"Mariana S.", "Atendimento impecável e o agendamento pelo site é muito prático. "
                    + "Meu corte ficou exatamente como eu queria!", "6", "avaliacao-1"},
            new String[] {"Juliana R.", "Fiz a hidratação e meus cachos nunca ficaram tão definidos. "
                    + "Equipe atenciosa e pontual.", "12", "avaliacao-2"},
            new String[] {"Fernanda L.", "Amei escolher a profissional e o horário pelo celular. "
                    + "As unhas em gel duraram semanas!", "21", "avaliacao-3"},
            new String[] {"Camila P.", "Ambiente lindo e acolhedor. Recebi o lembrete no WhatsApp e "
                    + "confirmei com um toque.", "34", "avaliacao-4"});

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
        // cada etapa protegida: uma imagem com problema nunca impede o sistema de subir
        etapa("salão", this::salao);
        etapa("serviços", () -> SERVICOS.forEach((nome, arq) -> jdbc.update(
                "UPDATE public.servicos SET foto_url = ? WHERE nome = ? AND foto_url IS NULL", url(arq), nome)));
        etapa("produtos", () -> PRODUTOS.forEach((nome, arq) -> jdbc.update(
                "UPDATE public.produtos SET foto_url = ? WHERE nome = ? AND foto_url IS NULL", url(arq), nome)));
        etapa("equipe", () -> EQUIPE.forEach((nome, arq) -> jdbc.update(
                "UPDATE public.profissionais SET foto_url = ? WHERE nome = ? AND foto_url IS NULL", url(arq), nome)));
        etapa("lookbook", this::lookbook);
        etapa("avaliações", this::avaliacoes);
        log.info("Demonstração: ilustrações de exemplo prontas");
    }

    private void etapa(String nome, Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException e) {
            log.warn("Demonstração: não foi possível preparar as imagens de {}: {}", nome, e.getMessage());
        }
    }

    private void salao() {
        // salão fictício (a demonstração não usa o nome de um cliente real)
        jdbc.update("""
                UPDATE public.configuracao_salao
                   SET nome = 'Studio Aurora', cidade = 'Florianópolis - SC', instagram = NULL,
                       slogan = COALESCE(slogan, 'Beleza com hora marcada')
                 WHERE id = 1 AND nome = 'Jo Fashion Hair'""");
        String[][] fotos = {
                {"logo_url", "salao-logo"},
                {"foto_hero_url", "salao-fundo-principal"},
                {"foto_espaco_url", "salao-espaco"},
                {"foto_visita_url", "salao-fachada"},
        };
        for (String[] f : fotos) {
            jdbc.update("UPDATE public.configuracao_salao SET " + f[0] + " = ? WHERE id = 1 AND " + f[0] + " IS NULL",
                    url(f[1]));
        }
    }

    private void lookbook() {
        Integer fotos = jdbc.queryForObject("SELECT COUNT(*) FROM public.lookbook_fotos", Integer.class);
        if (fotos != null && fotos == 0) {
            for (int i = 0; i < LOOKBOOK.size(); i++) {
                jdbc.update("INSERT INTO public.lookbook_fotos (foto_url, legenda, ordem) VALUES (?, ?, ?)",
                        url(LOOKBOOK.get(i)[0]), LOOKBOOK.get(i)[1], i + 1);
            }
        }
    }

    /** Avaliações de exemplo: sem elas, a seção "Avaliações" fica escondida no site. */
    private void avaliacoes() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM public.avaliacoes", Integer.class);
        if (total != null && total == 0) {
            String[][] exemplos = {
                    {"Mariana S.", "Atendimento impecável! Agendei pelo site em um minuto e recebi o lembrete no WhatsApp. "
                            + "O corte ficou exatamente como eu queria.", "cliente-mariana", "12"},
                    {"Juliana R.", "Ambiente lindo e acolhedor. A Bruna é uma artista nas unhas em gel, "
                            + "duraram mais de três semanas perfeitas.", "cliente-juliana", "25"},
                    {"Patrícia M.", "Fiz mechas e hidratação e amei o resultado. Pontualidade e cuidado "
                            + "do começo ao fim. Já marquei o próximo horário!", "cliente-patricia", "40"},
            };
            for (int i = 0; i < exemplos.length; i++) {
                jdbc.update("""
                        INSERT INTO public.avaliacoes (autor, texto, nota, data, foto_url, ordem)
                        VALUES (?, ?, 5, CURRENT_DATE - ?, ?, ?)""",
                        exemplos[i][0], exemplos[i][1], Integer.parseInt(exemplos[i][3]), url(exemplos[i][2]), i + 1);
            }
        }
        jdbc.update("""
                UPDATE public.configuracao_salao
                   SET avaliacoes_nota = 4.9, avaliacoes_total = 128
                 WHERE id = 1 AND avaliacoes_nota IS NULL""");
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
