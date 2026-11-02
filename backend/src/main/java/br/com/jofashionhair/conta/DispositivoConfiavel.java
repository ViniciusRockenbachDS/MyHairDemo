package br.com.jofashionhair.conta;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Navegador/aparelho confirmado por código de e-mail. Só o hash do token fica no banco. */
@Entity
@Table(name = "dispositivos_confiaveis")
public class DispositivoConfiavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private UsuarioAdmin usuario;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 64)
    private String ip;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "ultimo_uso_em", nullable = false)
    private OffsetDateTime ultimoUsoEm;

    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    protected DispositivoConfiavel() {
    }

    public DispositivoConfiavel(UsuarioAdmin usuario, String tokenHash, String nome, String ip,
                                OffsetDateTime agora, OffsetDateTime expiraEm) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.nome = nome;
        this.ip = ip;
        this.criadoEm = agora;
        this.ultimoUsoEm = agora;
        this.expiraEm = expiraEm;
    }

    public void registrarUso(OffsetDateTime agora) {
        this.ultimoUsoEm = agora;
    }

    public Long getId() { return id; }
    public UsuarioAdmin getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public String getNome() { return nome; }
    public String getIp() { return ip; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public OffsetDateTime getUltimoUsoEm() { return ultimoUsoEm; }
    public OffsetDateTime getExpiraEm() { return expiraEm; }
}
