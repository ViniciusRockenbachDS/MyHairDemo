package br.com.jofashionhair.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Agendamento;
import br.com.jofashionhair.domain.StatusAgendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    /**
     * QUERY DE VALIDAÇÃO DE CONFLITO (double booking).
     *
     * <p>Dois intervalos [A1, A2) e [B1, B2) se sobrepõem quando
     * {@code A1 < B2 AND A2 > B1}. O fim do pedido é calculado com a duração
     * ATUAL do serviço; o fim dos agendamentos existentes vem do snapshot deles.</p>
     *
     * <p>Use {@code ignorarId = 0} ao criar; ao remarcar, passe o id do próprio
     * agendamento para ele não conflitar consigo mesmo.</p>
     *
     * <p>Esta checagem dá a resposta amigável; a garantia final contra corrida
     * é a constraint {@code ex_agendamentos_sem_conflito} do banco.</p>
     */
    @Query("""
            select count(a) > 0 from Agendamento a
             where a.profissional.id = :profissionalId
               and a.status in :statusAtivos
               and a.dataInicio < :fim
               and a.dataFim > :inicio
               and a.id <> :ignorarId
            """)
    boolean existeConflito(@Param("profissionalId") Long profissionalId,
                           @Param("statusAtivos") Collection<StatusAgendamento> statusAtivos,
                           @Param("inicio") OffsetDateTime inicio,
                           @Param("fim") OffsetDateTime fim,
                           @Param("ignorarId") Long ignorarId);

    /** Agendamentos que ocupam a agenda da profissional dentro de um período (ex.: o dia todo). */
    @Query("""
            select a from Agendamento a
             where a.profissional.id = :profissionalId
               and a.status in :statusAtivos
               and a.dataInicio < :fim
               and a.dataFim > :inicio
             order by a.dataInicio
            """)
    List<Agendamento> findOcupandoPeriodo(@Param("profissionalId") Long profissionalId,
                                          @Param("statusAtivos") Collection<StatusAgendamento> statusAtivos,
                                          @Param("inicio") OffsetDateTime inicio,
                                          @Param("fim") OffsetDateTime fim);

    /** Agenda do /admin: todos os agendamentos que COMEÇAM no período, já com cliente/profissional/serviço. */
    @Query("""
            select a from Agendamento a
              join fetch a.cliente
              join fetch a.profissional
              join fetch a.servico
             where a.dataInicio >= :inicio
               and a.dataInicio < :fim
             order by a.dataInicio
            """)
    List<Agendamento> findPeriodoComDetalhes(@Param("inicio") OffsetDateTime inicio,
                                             @Param("fim") OffsetDateTime fim);

    @Query("""
            select a from Agendamento a
              join fetch a.cliente
              join fetch a.profissional
              join fetch a.servico
             where a.id = :id
            """)
    Optional<Agendamento> findByIdComDetalhes(@Param("id") Long id);

    /** Histórico completo de um cliente (mais recentes primeiro). */
    @Query("""
            select a from Agendamento a
              join fetch a.cliente
              join fetch a.profissional
              join fetch a.servico
             where a.cliente.id = :clienteId
             order by a.dataInicio desc
            """)
    List<Agendamento> findDoClienteComDetalhes(@Param("clienteId") Long clienteId);

    /** Próximos agendamentos ativos de um cliente (robô: "cancelar"). */
    @Query("""
            select a from Agendamento a
              join fetch a.cliente c
              join fetch a.profissional
              join fetch a.servico
             where c.telefone = :telefone
               and a.status in :status
               and a.dataInicio > :agora
             order by a.dataInicio
            """)
    List<Agendamento> findFuturosDoTelefone(@Param("telefone") String telefone,
                                            @Param("status") Collection<StatusAgendamento> status,
                                            @Param("agora") OffsetDateTime agora);

    // ===================================================================== lembretes (Passo 5)

    /**
     * Agendamentos que precisam do 1º lembrete (ex.: 24h antes): ativos, ainda
     * sem lembrete, começando dentro da janela, e marcados com MAIS antecedência
     * que o próprio lembrete (quem marcou ontem para hoje não recebe o de 24h).
     */
    @Query(value = """
            select a.id from agendamentos a
             where a.status in ('PENDENTE', 'CONFIRMADO')
               and a.lembrete_24h_enviado_em is null
               and a.data_inicio > :minimo
               and a.data_inicio <= :maximo
               and a.created_at < a.data_inicio - make_interval(hours => :horas)
             order by a.data_inicio
            """, nativeQuery = true)
    List<Long> idsParaPrimeiroLembrete(@Param("minimo") OffsetDateTime minimo,
                                       @Param("maximo") OffsetDateTime maximo,
                                       @Param("horas") int horasAntecedencia);

    /** Idem para o 2º lembrete (ex.: 2h antes). */
    @Query(value = """
            select a.id from agendamentos a
             where a.status in ('PENDENTE', 'CONFIRMADO')
               and a.lembrete_2h_enviado_em is null
               and a.data_inicio > :minimo
               and a.data_inicio <= :maximo
               and a.created_at < a.data_inicio - make_interval(hours => :horas)
             order by a.data_inicio
            """, nativeQuery = true)
    List<Long> idsParaSegundoLembrete(@Param("minimo") OffsetDateTime minimo,
                                      @Param("maximo") OffsetDateTime maximo,
                                      @Param("horas") int horasAntecedencia);

    /**
     * "Reserva" o envio de forma atômica: só UMA execução consegue marcar o
     * lembrete (retorno 1). Evita mensagens duplicadas mesmo com várias
     * instâncias da aplicação ou execuções sobrepostas.
     */
    @Transactional
    @Modifying
    @Query(value = """
            update agendamentos set lembrete_24h_enviado_em = :agora
             where id = :id and lembrete_24h_enviado_em is null and status in ('PENDENTE', 'CONFIRMADO')
            """, nativeQuery = true)
    int marcarPrimeiroLembrete(@Param("id") Long id, @Param("agora") OffsetDateTime agora);

    @Transactional
    @Modifying
    @Query(value = """
            update agendamentos set lembrete_2h_enviado_em = :agora
             where id = :id and lembrete_2h_enviado_em is null and status in ('PENDENTE', 'CONFIRMADO')
            """, nativeQuery = true)
    int marcarSegundoLembrete(@Param("id") Long id, @Param("agora") OffsetDateTime agora);

    /** Desfaz a marcação quando o envio falhou (a próxima execução tenta de novo). */
    @Transactional
    @Modifying
    @Query(value = "update agendamentos set lembrete_24h_enviado_em = null where id = :id", nativeQuery = true)
    int desmarcarPrimeiroLembrete(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = "update agendamentos set lembrete_2h_enviado_em = null where id = :id", nativeQuery = true)
    int desmarcarSegundoLembrete(@Param("id") Long id);

    // ===================================================================== no-show

    /** Atendimentos que já terminaram mas continuam PENDENTE/CONFIRMADO: falta dar baixa (concluído ou faltou). */
    @Query("""
            select a from Agendamento a
              join fetch a.cliente
              join fetch a.profissional
              join fetch a.servico
             where a.status in :status
               and a.dataFim < :agora
               and a.dataInicio >= :desde
             order by a.dataInicio
            """)
    List<Agendamento> findSemBaixa(@Param("status") Collection<StatusAgendamento> status,
                                   @Param("agora") OffsetDateTime agora,
                                   @Param("desde") OffsetDateTime desde);

    /** LGPD: apaga todos os agendamentos de uma cliente (exclusão definitiva). */
    @Modifying(flushAutomatically = true)
    @Query("delete from Agendamento a where a.cliente.id = :clienteId")
    int apagarDoCliente(@Param("clienteId") Long clienteId);

    // ===================================================================== promoções

    /** Clientes com pelo menos um atendimento concluído. */
    @Query("""
            select distinct a.cliente from Agendamento a
             where a.status = br.com.jofashionhair.domain.StatusAgendamento.CONCLUIDO
            """)
    List<br.com.jofashionhair.domain.Cliente> findClientesComHistorico();

    /** Idem, mas só quem já fez algum destes serviços. */
    @Query("""
            select distinct a.cliente from Agendamento a
             where a.status = br.com.jofashionhair.domain.StatusAgendamento.CONCLUIDO
               and a.servico.id in :servicoIds
            """)
    List<br.com.jofashionhair.domain.Cliente> findClientesComHistoricoNosServicos(
            @Param("servicoIds") Collection<Long> servicoIds);

    // ===================================================================== sinal

    java.util.Optional<Agendamento> findByPagamentoId(String pagamentoId);

    java.util.Optional<Agendamento> findByCodigoPublico(java.util.UUID codigoPublico);

    @Query("""
            select a from Agendamento a
             where a.status = br.com.jofashionhair.domain.StatusAgendamento.AGUARDANDO_SINAL
               and a.sinalExpiraEm < :agora
            """)
    List<Agendamento> findSinaisVencidos(@Param("agora") java.time.OffsetDateTime agora);

    /** Sinais em aberto com Pix do Mercado Pago (conferidos a cada minuto). */
    @Query("""
            select a from Agendamento a
             where a.status = br.com.jofashionhair.domain.StatusAgendamento.AGUARDANDO_SINAL
               and a.pagamentoId is not null
            """)
    List<Agendamento> findSinaisEmAbertoComPagamento();
}
