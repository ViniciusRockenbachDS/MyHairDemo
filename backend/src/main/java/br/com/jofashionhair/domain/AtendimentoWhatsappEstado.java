package br.com.jofashionhair.domain;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Estado persistido da conversa de um telefone (a "memória" do robô).
 * Sobrevive a reinícios da aplicação porque fica no PostgreSQL.
 */
@Entity
@Table(name = "atendimento_whatsapp_estado")
public class AtendimentoWhatsappEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoConversa estado = EstadoConversa.INICIAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private ContextoConversa contexto = new ContextoConversa();

    @Column(name = "ultima_interacao_em", nullable = false)
    private OffsetDateTime ultimaInteracaoEm = OffsetDateTime.now();

    /** Quando o estado atual deixa de valer (conversa abandonada, pausa humana, lembrete). */
    @Column(name = "expira_em")
    private OffsetDateTime expiraEm;

    protected AtendimentoWhatsappEstado() {
    }

    public AtendimentoWhatsappEstado(String telefone) {
        this.telefone = telefone;
    }

    public boolean expirada(OffsetDateTime agora) {
        return expiraEm != null && agora.isAfter(expiraEm);
    }

    public void registrarInteracao(OffsetDateTime agora, int horasParaExpirar) {
        this.ultimaInteracaoEm = agora;
        this.expiraEm = agora.plusHours(horasParaExpirar);
    }

    public void reiniciar() {
        this.estado = EstadoConversa.INICIAL;
        this.contexto = new ContextoConversa();
    }

    public void finalizar() {
        this.estado = EstadoConversa.FINALIZADO;
        this.contexto = new ContextoConversa();
    }

    public void pausarParaAtendimentoHumano(OffsetDateTime ate) {
        this.estado = EstadoConversa.ATENDIMENTO_HUMANO;
        this.expiraEm = ate;
    }

    /** Usado pelo lembrete (Passo 5): a próxima resposta do cliente é sim/não. */
    public void aguardarConfirmacaoLembrete(Long agendamentoId, OffsetDateTime ate) {
        this.estado = EstadoConversa.AGUARDANDO_CONFIRMACAO_LEMBRETE;
        this.contexto = new ContextoConversa();
        this.contexto.setAgendamentoLembreteId(agendamentoId);
        this.expiraEm = ate;
    }

    /** Depois de cancelar, já deixa serviço/profissional prontos para remarcar. */
    public void prepararRemarcacao(Long servicoId, Long profissionalId) {
        this.contexto = new ContextoConversa();
        this.contexto.escolherServico(servicoId);
        this.contexto.escolherProfissional(profissionalId);
        this.estado = EstadoConversa.AGUARDANDO_DATA_HORA;
    }

    public Long getId() { return id; }
    public String getTelefone() { return telefone; }
    public EstadoConversa getEstado() { return estado; }
    public void setEstado(EstadoConversa estado) { this.estado = estado; }
    public ContextoConversa getContexto() { return contexto; }
    public OffsetDateTime getUltimaInteracaoEm() { return ultimaInteracaoEm; }
    public OffsetDateTime getExpiraEm() { return expiraEm; }
}
