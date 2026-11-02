package br.com.jofashionhair.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.jofashionhair.domain.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    /** Serviços exibidos no site e oferecidos pelo robô. */
    List<Servico> findByAtivoTrueOrderByOrdemExibicaoAscNomeAsc();

    /** Usado pelo robô quando o cliente digita o nome do serviço. */
    Optional<Servico> findFirstByNomeIgnoreCaseAndAtivoTrue(String nome);

    /** Serviços ativos que têm pelo menos uma profissional ativa (menu do robô). */
    @Query("""
            select distinct s from Profissional p
              join p.servicos s
             where p.ativo = true
               and s.ativo = true
             order by s.ordemExibicao, s.nome
            """)
    List<Servico> findAtivosComProfissional();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
