package br.com.jofashionhair.demo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.jofashionhair.config.AppProperties;

/**
 * Vagas de demonstração: um schema (demo_1 ... demo_N) por visitante.
 *
 * <ul>
 *   <li>Na primeira visita, a vaga livre (ou a mais antiga, já expirada) é
 *       reservada e recebe uma cópia fresca dos dados originais (schema public).</li>
 *   <li>Sem uso por {@code horasSessao}, a vaga volta a ficar livre.</li>
 *   <li>Os dados originais nunca são alterados por visitantes.</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class SessoesDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SessoesDemo.class);
    public static final String EMAIL_VISITANTE = "visitante@demo.myhair";

    /** Tabelas que NÃO vão para a cópia do visitante (contas, códigos, conversas). */
    private static final Set<String> NAO_COPIAR = Set.of("usuarios_admin", "codigos_verificacao",
            "dispositivos_confiaveis", "campanhas_whatsapp", "atendimento_whatsapp_estado",
            "whatsapp_mensagens_processadas");

    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final int maxSessoes;
    private final int horas;

    public SessoesDemo(JdbcTemplate jdbc, DataSource dataSource, AppProperties props) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.maxSessoes = Math.max(1, props.demo().maxSessoes());
        this.horas = Math.max(1, props.demo().horasSessao());
    }

    public int horas() {
        return horas;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.demo_sessoes (
                    numero        INTEGER     PRIMARY KEY,
                    sessao        VARCHAR(36) UNIQUE,
                    ultimo_acesso TIMESTAMPTZ,
                    migrado       BOOLEAN     NOT NULL DEFAULT FALSE
                )""");
        for (int n = 1; n <= maxSessoes; n++) {
            jdbc.update("INSERT INTO public.demo_sessoes (numero) VALUES (?) ON CONFLICT DO NOTHING", n);
        }
        // a cada publicação/reinício, todo visitante recebe uma cópia nova dos dados originais
        // (assim ninguém fica preso a uma versão antiga, sem as novidades)
        jdbc.update("UPDATE public.demo_sessoes SET sessao = NULL WHERE sessao IS NOT NULL");
        // Nova versão publicada (ou servidor reiniciado): descarta as cópias antigas para todo visitante
        // receber os dados atuais (com as imagens e textos mais recentes).
        int liberadas = jdbc.update("UPDATE public.demo_sessoes SET sessao = NULL WHERE sessao IS NOT NULL");
        log.info("Modo demonstração ligado: até {} visitantes ao mesmo tempo, {} h por visita ({} cópia(s) antiga(s) descartada(s))",
                maxSessoes, horas, liberadas);
    }

    /** Schema da sessão, se ela existe (e marca o uso, no máximo 1x por minuto). */
    public Optional<String> esquemaDaSessao(String sessao) {
        List<Integer> r = jdbc.queryForList("""
                UPDATE public.demo_sessoes
                   SET ultimo_acesso = CASE WHEN ultimo_acesso < now() - interval '1 minute' THEN now() ELSE ultimo_acesso END
                 WHERE sessao = ?
                RETURNING numero""", Integer.class, sessao);
        return r.isEmpty() ? Optional.empty() : Optional.of("demo_" + r.get(0));
    }

    /** Garante uma vaga para a sessão (preparando a cópia, se for nova). */
    public synchronized String iniciar(String sessao) {
        Optional<String> existente = esquemaDaSessao(sessao);
        if (existente.isPresent()) {
            return existente.get();
        }
        List<Map<String, Object>> vaga = jdbc.queryForList("""
                UPDATE public.demo_sessoes SET sessao = ?, ultimo_acesso = now()
                 WHERE numero = (
                        SELECT numero FROM public.demo_sessoes
                         WHERE sessao IS NULL OR ultimo_acesso < now() - (? * interval '1 hour')
                         ORDER BY (sessao IS NOT NULL), ultimo_acesso NULLS FIRST
                         LIMIT 1 FOR UPDATE SKIP LOCKED)
                RETURNING numero, migrado""", sessao, horas);
        if (vaga.isEmpty()) {
            throw new DemoLotadaException();
        }
        int numero = ((Number) vaga.get(0).get("numero")).intValue();
        boolean migrado = Boolean.TRUE.equals(vaga.get(0).get("migrado"));
        String schema = "demo_" + numero;
        try {
            if (!migrado) {
                criarEstrutura(schema);
                jdbc.update("UPDATE public.demo_sessoes SET migrado = TRUE WHERE numero = ?", numero);
            }
            copiarDadosOriginais(schema);
        } catch (RuntimeException e) {
            jdbc.update("UPDATE public.demo_sessoes SET sessao = NULL WHERE numero = ?", numero);
            throw e;
        }
        return schema;
    }

    /** A cada 10 minutos: libera vagas paradas e apaga imagens enviadas por visitantes antigos. */
    @Scheduled(fixedDelay = 600_000, initialDelay = 120_000)
    public void liberarVagasParadas() {
        int vagas = jdbc.update("""
                UPDATE public.demo_sessoes SET sessao = NULL
                 WHERE sessao IS NOT NULL AND ultimo_acesso < now() - (? * interval '1 hour')""", horas);
        int imagens = jdbc.update("""
                DELETE FROM public.arquivos
                 WHERE origem = 'VISITANTE' AND criado_em < now() - (? * interval '1 hour')""", horas + 1);
        if (vagas + imagens > 0) {
            log.info("Demonstração: {} vaga(s) liberada(s), {} imagem(ns) de visitantes apagada(s)", vagas, imagens);
        }
    }

    // ================================================================== cópia

    /** Cria as tabelas no schema da vaga (as mesmas migrações do sistema, sem dados de exemplo). */
    private void criarEstrutura(String schema) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .createSchemas(true)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    /** Apaga a vaga e copia os dados originais (public) para ela. */
    private void copiarDadosOriginais(String schema) {
        List<String> tabelas = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                 WHERE table_schema = ? AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'""",
                String.class, schema);
        if (tabelas.isEmpty()) {
            return;
        }
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", tabelas.stream().map(t -> q(schema) + "." + q(t)).toList())
                + " RESTART IDENTITY CASCADE");

        for (String tabela : ordemPorDependencia(schema, tabelas)) {
            if (NAO_COPIAR.contains(tabela)) {
                continue;
            }
            List<String> colunas = colunasEmComum(schema, tabela);
            if (colunas.isEmpty()) {
                continue;
            }
            String lista = String.join(", ", colunas.stream().map(ConfiguracaoNomes::q).toList());
            jdbc.execute("INSERT INTO " + q(schema) + "." + q(tabela) + " (" + lista + ") SELECT " + lista
                    + " FROM public." + q(tabela));
        }

        // o visitante entra no painel como dono da sua cópia
        jdbc.update("INSERT INTO " + q(schema) + ".usuarios_admin (nome, email, senha_hash, papel, ativo) "
                + "VALUES ('Visitante da demonstração', ?, '!', 'DONO', TRUE)", EMAIL_VISITANTE);

        // as próximas inserções continuam a numeração a partir dos dados copiados
        List<Map<String, Object>> sequencias = jdbc.queryForList("""
                SELECT s.relname AS seq, t.relname AS tabela, a.attname AS coluna
                  FROM pg_class s
                  JOIN pg_namespace n ON n.oid = s.relnamespace
                  JOIN pg_depend d ON d.objid = s.oid AND d.deptype = 'a'
                  JOIN pg_class t ON t.oid = d.refobjid
                  JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = d.refobjsubid
                 WHERE s.relkind = 'S' AND n.nspname = ?""", schema);
        for (Map<String, Object> s : sequencias) {
            String seq = q(schema) + "." + q((String) s.get("seq"));
            String tabela = q(schema) + "." + q((String) s.get("tabela"));
            String coluna = q((String) s.get("coluna"));
            jdbc.execute("SELECT setval('" + seq.replace("'", "''") + "', COALESCE((SELECT MAX(" + coluna + ") FROM "
                    + tabela + "), 0) + 1, false)");
        }
    }

    private List<String> colunasEmComum(String schema, String tabela) {
        Set<String> originais = new HashSet<>(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = ?""", String.class, tabela));
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_schema = ? AND table_name = ? AND is_generated = 'NEVER'
                 ORDER BY ordinal_position""", String.class, schema, tabela)
                .stream().filter(originais::contains).toList();
    }

    /** Tabelas "mãe" antes das "filhas" (chaves estrangeiras). */
    private List<String> ordemPorDependencia(String schema, List<String> tabelas) {
        Map<String, Set<String>> pais = new HashMap<>();
        tabelas.forEach(t -> pais.put(t, new HashSet<>()));
        jdbc.query("""
                SELECT DISTINCT tc.table_name AS filha, ccu.table_name AS mae
                  FROM information_schema.table_constraints tc
                  JOIN information_schema.constraint_column_usage ccu
                    ON ccu.constraint_name = tc.constraint_name AND ccu.constraint_schema = tc.constraint_schema
                 WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = ?""",
                rs -> {
                    String filha = rs.getString("filha");
                    String mae = rs.getString("mae");
                    if (pais.containsKey(filha) && pais.containsKey(mae) && !filha.equals(mae)) {
                        pais.get(filha).add(mae);
                    }
                }, schema);
        List<String> ordem = new ArrayList<>();
        Set<String> feitas = new LinkedHashSet<>();
        Deque<String> pendentes = new ArrayDeque<>(tabelas);
        int voltasSemProgresso = 0;
        while (!pendentes.isEmpty() && voltasSemProgresso <= pendentes.size()) {
            String t = pendentes.poll();
            if (feitas.containsAll(pais.get(t))) {
                ordem.add(t);
                feitas.add(t);
                voltasSemProgresso = 0;
            } else {
                pendentes.add(t);
                voltasSemProgresso++;
            }
        }
        ordem.addAll(pendentes);   // ciclo (não esperado): vai no fim
        return ordem;
    }

    private static String q(String identificador) {
        return ConfiguracaoNomes.q(identificador);
    }

    /** Aspas em nomes de tabela/coluna vindos do catálogo do PostgreSQL. */
    static final class ConfiguracaoNomes {
        private ConfiguracaoNomes() {
        }

        static String q(String identificador) {
            return "\"" + identificador.replace("\"", "\"\"") + "\"";
        }
    }
}
