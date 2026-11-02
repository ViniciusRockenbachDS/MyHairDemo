package br.com.jofashionhair.conta;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioAdminRepository extends JpaRepository<UsuarioAdmin, Long> {

    Optional<UsuarioAdmin> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UsuarioAdmin> findAllByOrderByPapelAscNomeAsc();

    long countByPapelAndAtivoTrue(PapelUsuario papel);
}
