package br.com.jofashionhair.conta;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login do painel: e-mail + senha conferidos contra o hash BCrypt do banco.
 * Acesso desativado pelo dono deixa de funcionar na hora.
 */
@Service
public class UsuarioAdminDetailsService implements UserDetailsService {

    private final UsuarioAdminRepository repository;

    public UsuarioAdminDetailsService(UsuarioAdminRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        UsuarioAdmin u = repository.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha incorretos."));
        return User.withUsername(u.getEmail())
                .password(u.getSenhaHash())
                .roles(u.getPapel().name())
                .disabled(!u.isAtivo())
                .build();
    }
}
