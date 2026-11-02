package br.com.jofashionhair.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Produto da vitrine do site (venda no salão; interesse pelo WhatsApp). */
@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 80)
    private String marca;

    @Column(columnDefinition = "text")
    private String descricao;

    /** Vazio = "Consulte no salão". */
    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    /** false = esgotado (continua aparecendo, com o aviso). */
    @Column(nullable = false)
    private boolean disponivel = true;

    @Column(nullable = false)
    private Integer ordem = 0;

    /** false = escondido do site. */
    @Column(nullable = false)
    private boolean ativo = true;

    public Produto() {
    }

    public void atualizar(String nome, String marca, String descricao, BigDecimal preco, String fotoUrl,
                          boolean disponivel, int ordem, boolean ativo) {
        this.nome = nome;
        this.marca = marca;
        this.descricao = descricao;
        this.preco = preco;
        this.fotoUrl = fotoUrl;
        this.disponivel = disponivel;
        this.ordem = ordem;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getMarca() { return marca; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public String getFotoUrl() { return fotoUrl; }
    public boolean isDisponivel() { return disponivel; }
    public Integer getOrdem() { return ordem; }
    public boolean isAtivo() { return ativo; }
}
