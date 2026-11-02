package br.com.jofashionhair.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Serviço do salão. Preço e duração são lidos do banco a cada consulta de
 * disponibilidade, então uma alteração no /admin vale imediatamente para
 * novos agendamentos (os já feitos guardam seu próprio snapshot).
 */
@Entity
@Table(name = "servicos")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(nullable = false, length = 40)
    private String categoria = "OUTROS";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(name = "ordem_exibicao", nullable = false)
    private Integer ordemExibicao = 0;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Cobra sinal por Pix para garantir o horário (site e WhatsApp). */
    @Column(name = "exige_sinal", nullable = false)
    private boolean exigeSinal;

    @Column(name = "valor_sinal", precision = 10, scale = 2)
    private BigDecimal valorSinal;

    /** Foto exibida no card do serviço no site (vazio = imagem de exemplo). */
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    protected Servico() {
    }

    public Servico(String nome, String categoria, BigDecimal preco, Integer duracaoMinutos) {
        this.nome = nome;
        this.categoria = categoria;
        this.preco = preco;
        this.duracaoMinutos = duracaoMinutos;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPreco() { return preco; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public Integer getOrdemExibicao() { return ordemExibicao; }
    public boolean isAtivo() { return ativo; }
    public String getFotoUrl() { return fotoUrl; }
    public boolean isExigeSinal() { return exigeSinal; }
    public BigDecimal getValorSinal() { return valorSinal; }
    public void definirSinal(boolean exige, BigDecimal valor) {
        this.exigeSinal = exige;
        this.valorSinal = exige ? valor : null;
    }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public void setNome(String nome) { this.nome = nome; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public void setOrdemExibicao(Integer ordemExibicao) { this.ordemExibicao = ordemExibicao; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Servico other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Servico.class.hashCode();
    }
}
