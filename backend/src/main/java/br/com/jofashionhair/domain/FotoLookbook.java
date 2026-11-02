package br.com.jofashionhair.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Foto de um trabalho feito no salão (seção Lookbook do site). */
@Entity
@Table(name = "lookbook_fotos")
public class FotoLookbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "foto_url", nullable = false, length = 500)
    private String fotoUrl;

    @Column(length = 120)
    private String legenda;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    public FotoLookbook() {
    }

    public void atualizar(String fotoUrl, String legenda, int ordem, boolean ativo) {
        this.fotoUrl = fotoUrl;
        this.legenda = legenda;
        this.ordem = ordem;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getFotoUrl() { return fotoUrl; }
    public String getLegenda() { return legenda; }
    public Integer getOrdem() { return ordem; }
    public boolean isAtivo() { return ativo; }
}
