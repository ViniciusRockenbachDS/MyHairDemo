package br.com.jofashionhair.conta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pessoa com acesso ao painel /admin. A senha fica só como hash BCrypt. */
@Entity
@Table(name = "usuarios_admin")
public class UsuarioAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelUsuario papel;

    @Column(nullable = false)
    private boolean ativo = true;

    protected UsuarioAdmin() {
    }

    public UsuarioAdmin(String nome, String email, String senhaHash, PapelUsuario papel) {
        this.nome = nome;
        this.email = email.trim().toLowerCase();
        this.senhaHash = senhaHash;
        this.papel = papel;
    }

    public void definirSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public PapelUsuario getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }

    public void setNome(String nome) { this.nome = nome; }
    public void setPapel(PapelUsuario papel) { this.papel = papel; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
