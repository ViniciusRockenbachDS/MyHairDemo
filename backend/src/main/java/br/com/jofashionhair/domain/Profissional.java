package br.com.jofashionhair.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "profissionais")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 120)
    private String especialidade;

    @Column(length = 20)
    private String telefone;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Texto curto exibido no card da equipe no site. */
    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    /** Relacionamento N:N com serviços (tabela profissional_servicos). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "profissional_servicos",
            joinColumns = @JoinColumn(name = "profissional_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id"))
    private Set<Servico> servicos = new HashSet<>();

    protected Profissional() {
    }

    public Profissional(String nome, String especialidade) {
        this.nome = nome;
        this.especialidade = especialidade;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEspecialidade() { return especialidade; }
    public String getTelefone() { return telefone; }
    public boolean isAtivo() { return ativo; }
    public Set<Servico> getServicos() { return servicos; }
    public String getBio() { return bio; }
    public String getFotoUrl() { return fotoUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public void setNome(String nome) { this.nome = nome; }
    public void setEspecialidade(String especialidade) { this.especialidade = especialidade; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public void vincularServico(Servico servico) { servicos.add(servico); }
    public void desvincularServico(Servico servico) { servicos.remove(servico); }
}
