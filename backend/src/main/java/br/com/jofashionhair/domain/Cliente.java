package br.com.jofashionhair.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Só dígitos com DDI (ver {@link Telefone#normalizar(String)}). */
    /** Fica nulo depois da anonimização (LGPD). */
    @Column(length = 20, unique = true)
    private String telefone;

    @Column(length = 160)
    private String email;

    @Column(columnDefinition = "text")
    private String observacoes;

    /** true = autorizou promoções; false = respondeu SAIR; null = não informou. */
    @Column(name = "aceita_promocoes")
    private Boolean aceitaPromocoes;

    @Column(name = "anonimizado_em")
    private java.time.OffsetDateTime anonimizadoEm;

    protected Cliente() {
        // exigido pelo JPA
    }

    public Cliente(String nome, String telefone) {
        this.nome = nome;
        this.telefone = Telefone.normalizar(telefone);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
    public String getObservacoes() { return observacoes; }

    public java.time.OffsetDateTime getAnonimizadoEm() { return anonimizadoEm; }

    public Boolean getAceitaPromocoes() { return aceitaPromocoes; }
    public void setAceitaPromocoes(Boolean aceitaPromocoes) { this.aceitaPromocoes = aceitaPromocoes; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
