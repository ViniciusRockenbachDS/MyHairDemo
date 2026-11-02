package br.com.jofashionhair.conta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.config.AppProperties;

/**
 * Na primeira vez que a API sobe (nenhum usuário no banco), cria a conta do
 * DONO com ADMIN_EMAIL / ADMIN_PASSWORD. Depois disso essas variáveis não são
 * mais usadas: a senha é trocada pelo painel, com código por e-mail.
 *
 * Emergência: ADMIN_FORCAR_SENHA=true redefine a senha do dono para
 * ADMIN_PASSWORD ao iniciar (útil se o e-mail estiver fora do ar).
 * Remova a variável logo depois.
 */
@Component
public class CriarDonoInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CriarDonoInicial.class);

    private final UsuarioAdminRepository repository;
    private final DispositivoConfiavelRepository dispositivoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties.Admin config;

    public CriarDonoInicial(UsuarioAdminRepository repository, DispositivoConfiavelRepository dispositivoRepository,
                            PasswordEncoder passwordEncoder, AppProperties props) {
        this.repository = repository;
        this.dispositivoRepository = dispositivoRepository;
        this.passwordEncoder = passwordEncoder;
        this.config = props.admin();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() == 0) {
            if (config == null || vazio(config.email()) || vazio(config.senha()) || config.senha().length() < 8) {
                throw new IllegalStateException("Primeiro acesso: defina ADMIN_EMAIL e ADMIN_PASSWORD "
                        + "(senha com pelo menos 8 caracteres) para criar a conta do dono.");
            }
            String nome = vazio(config.nome()) ? "Dono do salão" : config.nome().trim();
            repository.save(new UsuarioAdmin(nome, config.email(), passwordEncoder.encode(config.senha()),
                    PapelUsuario.DONO));
            log.info("Conta do dono criada: {}", config.email().trim().toLowerCase());
            return;
        }
        if (config != null && config.forcarSenha()) {
            if (vazio(config.email()) || vazio(config.senha()) || config.senha().length() < 8) {
                throw new IllegalStateException("ADMIN_FORCAR_SENHA exige ADMIN_EMAIL e ADMIN_PASSWORD válidos.");
            }
            repository.findByEmailIgnoreCase(config.email().trim()).ifPresentOrElse(u -> {
                u.definirSenhaHash(passwordEncoder.encode(config.senha()));
                u.setAtivo(true);
                dispositivoRepository.removerTodosExceto(u.getId(), "");
                log.warn("ADMIN_FORCAR_SENHA: senha de {} redefinida. REMOVA essa variável agora.", u.getEmail());
            }, () -> log.error("ADMIN_FORCAR_SENHA: nenhum usuário com o e-mail {}", config.email()));
        }
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
