package br.com.jofashionhair.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.jofashionhair.domain.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** O telefone deve vir normalizado (Telefone.normalizar). */
    Optional<Cliente> findByTelefone(String telefone);

    /**
     * Busca por nome OU telefone, com o resumo de comparecimento de cada cliente.
     *
     * @param nome    trecho do nome (vazio = todos)
     * @param digitos trecho do telefone, só dígitos (use "x" quando não houver dígitos)
     */
    @Query("""
            select new br.com.jofashionhair.repository.HistoricoCliente(
                       c.id, c.nome, c.telefone,
                       count(a),
                       count(case when a.status = br.com.jofashionhair.domain.StatusAgendamento.CONCLUIDO then 1 end),
                       count(case when a.status = br.com.jofashionhair.domain.StatusAgendamento.CANCELADO then 1 end),
                       count(case when a.status = br.com.jofashionhair.domain.StatusAgendamento.FALTOU then 1 end),
                       max(case when a.status = br.com.jofashionhair.domain.StatusAgendamento.FALTOU then a.dataInicio end))
              from Cliente c
              left join Agendamento a on a.cliente = c
             where lower(c.nome) like lower(concat('%', :nome, '%'))
                or c.telefone like concat('%', :digitos, '%')
             group by c.id, c.nome, c.telefone
             order by c.nome
            """)
    List<HistoricoCliente> buscarComHistorico(@Param("nome") String nome,
                                              @Param("digitos") String digitos,
                                              Pageable pageable);

    /**
     * Clientes a excluir pela retenção: cadastradas antes do limite e sem
     * nenhum agendamento a partir dele (nem passado recente, nem futuro).
     */
    @Query(value = """
            select c.id from clientes c
             where c.created_at < :limite
               and not exists (select 1 from agendamentos a
                                where a.cliente_id = c.id and a.data_inicio >= :limite)
            """, nativeQuery = true)
    List<Long> idsSemAtendimentoDesde(@Param("limite") OffsetDateTime limite);
}
