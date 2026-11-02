package br.com.jofashionhair.conta;

import java.time.OffsetDateTime;

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

/** Código de 6 dígitos enviado por e-mail. Só o hash fica gravado. */
@Entity
@Table(name = "codigos_verificacao")
public class CodigoVerificacao {

    public static final int MAX_TENTATIVAS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private UsuarioAdmin usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FinalidadeCodigo finalidade;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "usado_em")
    private OffsetDateTime usadoEm;

    protected CodigoVerificacao() {
    }

    public CodigoVerificacao(UsuarioAdmin usuario, FinalidadeCodigo finalidade, String codigoHash,
                             OffsetDateTime expiraEm) {
        this.usuario = usuario;
        this.finalidade = finalidade;
        this.codigoHash = codigoHash;
        this.expiraEm = expiraEm;
    }

    public boolean podeSerUsado(OffsetDateTime agora) {
        return usadoEm == null && agora.isBefore(expiraEm) && tentativas < MAX_TENTATIVAS;
    }

    public void registrarTentativaErrada() {
        tentativas++;
    }

    public void marcarUsado(OffsetDateTime agora) {
        usadoEm = agora;
    }

    public Long getId() { return id; }
    public UsuarioAdmin getUsuario() { return usuario; }
    public FinalidadeCodigo getFinalidade() { return finalidade; }
    public String getCodigoHash() { return codigoHash; }
    public OffsetDateTime getExpiraEm() { return expiraEm; }
    public int getTentativas() { return tentativas; }
}
