package br.com.jofashionhair.demo;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/**
 * Modo demonstração: cada visitante usa um schema próprio do PostgreSQL
 * (demo_1, demo_2...). O Hibernate escolhe o schema a cada transação pelo
 * {@link ContextoDemo}, definindo o search_path da conexão.
 */
@Configuration
@ConditionalOnProperty(name = "app.demo.habilitado", havingValue = "true")
public class ConfiguracaoMultiCopia {

    private static final Pattern SCHEMA_VALIDO = Pattern.compile("^(public|demo_\\d{1,3})$");

    @Bean
    public HibernatePropertiesCustomizer multiCopia(DataSource dataSource) {
        return props -> {
            props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, new Conexoes(dataSource));
            props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new QualCopia());
        };
    }

    /** Tarefas em segundo plano (@Async) continuam na cópia de quem as disparou. */
    @Bean
    public TaskDecorator propagarCopia() {
        return tarefa -> {
            ContextoDemo.Estado estado = ContextoDemo.capturar();
            return () -> {
                ContextoDemo.restaurar(estado);
                try {
                    tarefa.run();
                } finally {
                    ContextoDemo.limpar();
                }
            };
        };
    }

    static final class QualCopia implements CurrentTenantIdentifierResolver<String> {
        @Override
        public String resolveCurrentTenantIdentifier() {
            return ContextoDemo.schema();
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }
    }

    static final class Conexoes implements MultiTenantConnectionProvider<String> {

        private final transient DataSource dataSource;

        Conexoes(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Connection getAnyConnection() throws SQLException {
            return dataSource.getConnection();
        }

        @Override
        public void releaseAnyConnection(Connection conexao) throws SQLException {
            conexao.close();
        }

        @Override
        public Connection getConnection(String schema) throws SQLException {
            if (!SCHEMA_VALIDO.matcher(schema).matches()) {
                throw new SQLException("Schema inválido: " + schema);
            }
            Connection conexao = getAnyConnection();
            try (Statement st = conexao.createStatement()) {
                st.execute("SET search_path TO " + schema + ", public");
            }
            return conexao;
        }

        @Override
        public void releaseConnection(String schema, Connection conexao) throws SQLException {
            try (Statement st = conexao.createStatement()) {
                st.execute("SET search_path TO public");
            } finally {
                conexao.close();
            }
        }

        @Override
        public boolean supportsAggressiveRelease() {
            return false;
        }

        @Override
        public boolean isUnwrappableAs(Class<?> tipo) {
            return false;
        }

        @Override
        public <T> T unwrap(Class<T> tipo) {
            throw new UnknownUnwrapTypeException(tipo);
        }
    }
}
