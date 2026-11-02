package br.com.jofashionhair.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Avaliação de cliente cadastrada pelo dono no /admin e exibida no site. */
@Entity
@Table(name = "avaliacoes")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String autor;

    @Column(nullable = false, columnDefinition = "text")
    private String texto;

    @Column(nullable = false)
    private Short nota;

    /** Data da avaliação; o site mostra "há 5 meses". */
    private LocalDate data;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    /** Link para a avaliação original (ex.: no Google). */
    @Column(length = 500)
    private String link;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    public Avaliacao() {
    }

    public void atualizar(String autor, String texto, int nota, LocalDate data, String fotoUrl, String link,
                          int ordem, boolean ativo) {
        this.autor = autor;
        this.texto = texto;
        this.nota = (short) nota;
        this.data = data;
        this.fotoUrl = fotoUrl;
        this.link = link;
        this.ordem = ordem;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public String getAutor() { return autor; }
    public String getTexto() { return texto; }
    public Short getNota() { return nota; }
    public LocalDate getData() { return data; }
    public String getFotoUrl() { return fotoUrl; }
    public String getLink() { return link; }
    public Integer getOrdem() { return ordem; }
    public boolean isAtivo() { return ativo; }
}
