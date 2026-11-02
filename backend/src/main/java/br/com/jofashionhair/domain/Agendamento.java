package br.com.jofashionhair.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import br.com.jofashionhair.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Agendamento. Guarda um SNAPSHOT da duração e do preço do serviço no momento
 * da reserva: alterações futuras no serviço não mexem em reservas existentes.
 */
@Entity
@Table(name = "agendamentos")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id")
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id")
    private Servico servico;

    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private OffsetDateTime dataFim;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(name = "preco_cobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoCobrado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemAgendamento origem;

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(name = "lembrete_24h_enviado_em")
    private OffsetDateTime lembrete24hEnviadoEm;

    @Column(name = "lembrete_2h_enviado_em")
    private OffsetDateTime lembrete2hEnviadoEm;

    @Column(name = "confirmado_cliente_em")
    private OffsetDateTime confirmadoClienteEm;

    // ------------------------------------------------------------ sinal por Pix
    @Column(name = "valor_sinal", precision = 10, scale = 2)
    private BigDecimal valorSinal;

    @Column(name = "sinal_pago_em")
    private OffsetDateTime sinalPagoEm;

    @Column(name = "sinal_expira_em")
    private OffsetDateTime sinalExpiraEm;

    @Column(name = "pagamento_id", length = 80)
    private String pagamentoId;

    @Column(name = "pix_copia_cola", columnDefinition = "text")
    private String pixCopiaCola;

    @Column(name = "codigo_publico")
    private java.util.UUID codigoPublico;

    protected Agendamento() {
    }

    /**
     * Cria um agendamento copiando duração e preço ATUAIS do serviço.
     *
     * <p>Status inicial: reservas pelo WhatsApp e pelo /admin já nascem
     * CONFIRMADO; reservas pelo site nascem PENDENTE e são confirmadas pelo
     * cliente na mensagem de lembrete.</p>
     */
    public static Agendamento criar(Cliente cliente, Profissional profissional, Servico servico,
                                    OffsetDateTime inicio, OrigemAgendamento origem, String observacoes) {
        return criar(cliente, profissional, servico, inicio, origem, observacoes, servico.getPreco());
    }

    /** @param preco preço cobrado (com desconto de promoção, se houver) */
    public static Agendamento criar(Cliente cliente, Profissional profissional, Servico servico,
                                    OffsetDateTime inicio, OrigemAgendamento origem, String observacoes,
                                    BigDecimal preco) {
        Agendamento a = new Agendamento();
        a.cliente = cliente;
        a.profissional = profissional;
        a.servico = servico;
        a.dataInicio = inicio.truncatedTo(ChronoUnit.MINUTES);
        a.duracaoMinutos = servico.getDuracaoMinutos();
        a.dataFim = a.dataInicio.plusMinutes(a.duracaoMinutos);
        a.precoCobrado = preco;
        a.origem = origem;
        a.status = origem == OrigemAgendamento.SITE ? StatusAgendamento.PENDENTE : StatusAgendamento.CONFIRMADO;
        a.observacoes = observacoes;
        return a;
    }

    // ------------------------------------------------------------------ ciclo de vida

    public void alterarStatus(StatusAgendamento destino) {
        if (status == destino) {
            return;
        }
        if (!status.podeIrPara(destino)) {
            throw new RegraNegocioException(
                    "Não é possível alterar o agendamento de " + status + " para " + destino + ".");
        }
        this.status = destino;
    }

    /** Cliente respondeu "sim" ao lembrete. */
    public void confirmarPeloCliente(OffsetDateTime agora) {
        alterarStatus(StatusAgendamento.CONFIRMADO);
        this.confirmadoClienteEm = agora;
    }

    /** Segura o horário até o sinal ser pago (ou o prazo acabar). */
    public void aguardarSinal(BigDecimal valor, OffsetDateTime expiraEm) {
        this.status = StatusAgendamento.AGUARDANDO_SINAL;
        this.valorSinal = valor;
        this.sinalExpiraEm = expiraEm;
        this.codigoPublico = java.util.UUID.randomUUID();
    }

    public void registrarPix(String pagamentoId, String copiaCola) {
        this.pagamentoId = pagamentoId;
        this.pixCopiaCola = copiaCola;
    }

    /** Sinal pago: o horário fica confirmado. */
    public void registrarSinalPago(OffsetDateTime agora) {
        alterarStatus(StatusAgendamento.CONFIRMADO);
        this.sinalPagoEm = agora;
        this.confirmadoClienteEm = agora;
    }

    /** Pagamento chegou depois do prazo (horário já liberado): fica anotado para a equipe devolver ou remarcar. */
    public void registrarSinalPagoAposPrazo(OffsetDateTime agora) {
        this.sinalPagoEm = agora;
        String aviso = "Sinal pago depois do prazo: verificar devolução ou remarcação.";
        this.observacoes = observacoes == null ? aviso : observacoes + "\n" + aviso;
    }

    public void registrarLembrete24h(OffsetDateTime agora) { this.lembrete24hEnviadoEm = agora; }
    public void registrarLembrete2h(OffsetDateTime agora) { this.lembrete2hEnviadoEm = agora; }

    public boolean ocupaAgenda() {
        return StatusAgendamento.OCUPAM_AGENDA.contains(status);
    }

    // ------------------------------------------------------------------ getters

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Profissional getProfissional() { return profissional; }
    public Servico getServico() { return servico; }
    public OffsetDateTime getDataInicio() { return dataInicio; }
    public OffsetDateTime getDataFim() { return dataFim; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public BigDecimal getPrecoCobrado() { return precoCobrado; }
    public StatusAgendamento getStatus() { return status; }
    public OrigemAgendamento getOrigem() { return origem; }
    public String getObservacoes() { return observacoes; }
    public OffsetDateTime getLembrete24hEnviadoEm() { return lembrete24hEnviadoEm; }
    public OffsetDateTime getLembrete2hEnviadoEm() { return lembrete2hEnviadoEm; }
    public OffsetDateTime getConfirmadoClienteEm() { return confirmadoClienteEm; }

    public BigDecimal getValorSinal() { return valorSinal; }
    public OffsetDateTime getSinalPagoEm() { return sinalPagoEm; }
    public OffsetDateTime getSinalExpiraEm() { return sinalExpiraEm; }
    public String getPagamentoId() { return pagamentoId; }
    public String getPixCopiaCola() { return pixCopiaCola; }
    public java.util.UUID getCodigoPublico() { return codigoPublico; }
}
