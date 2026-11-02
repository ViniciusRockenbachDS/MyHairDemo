package br.com.jofashionhair.promocao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaWhatsappRepository extends JpaRepository<CampanhaWhatsapp, Long> {

    List<CampanhaWhatsapp> findTop30ByOrderByIdDesc();

    boolean existsByStatus(String status);
}
