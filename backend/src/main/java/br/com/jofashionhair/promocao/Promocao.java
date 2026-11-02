package br.com.jofashionhair.promocao;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.jofashionhair.domain.Servico;
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

/** Promoção: X% de desconto em certos dias da semana, período e serviços. */
@Entity
@Table(name = "promocoes")
public class Promocao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(columnDefinition = "text")
    private String descricao;

    @Column(name = "desconto_percentual", nullable = false)
    private Short descontoPercentual;

    /** "2,4" = terça e quinta (1 = segunda ... 7 = domingo). Vazio = todos os dias. */
    @Column(name = "dias_semana", nullable = false, length = 20)
    private String diasSemana = "";

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "todos_servicos", nullable = false)
    private boolean todosServicos = true;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "promocao_servicos",
            joinColumns = @JoinColumn(name = "promocao_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id"))
    private Set<Servico> servicos = new HashSet<>();

    public Promocao() {
    }

    /** A promoção vale para este serviço nesta data? */
    public boolean valePara(Long servicoId, LocalDate data) {
        if (!ativo) return false;
        if (dataInicio != null && data.isBefore(dataInicio)) return false;
        if (dataFim != null && data.isAfter(dataFim)) return false;
        List<Integer> dias = dias();
        if (!dias.isEmpty() && !dias.contains(data.getDayOfWeek().getValue())) return false;
        return todosServicos || servicos.stream().anyMatch(s -> s.getId().equals(servicoId));
    }

    /** Ainda pode valer (ativa e não terminou). */
    public boolean vigente(LocalDate hoje) {
        return ativo && (dataFim == null || !dataFim.isBefore(hoje));
    }

    public List<Integer> dias() {
        if (diasSemana == null || diasSemana.isBlank()) return List.of();
        return Arrays.stream(diasSemana.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::valueOf).sorted().toList();
    }

    public void atualizar(String titulo, String descricao, int desconto, List<Integer> dias, LocalDate inicio,
                          LocalDate fim, boolean todosServicos, Set<Servico> servicos, boolean ativo) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.descontoPercentual = (short) desconto;
        this.diasSemana = dias == null ? "" : String.join(",", dias.stream().distinct().sorted().map(String::valueOf).toList());
        this.dataInicio = inicio;
        this.dataFim = fim;
        this.todosServicos = todosServicos;
        this.servicos.clear();
        if (!todosServicos && servicos != null) this.servicos.addAll(servicos);
        this.ativo = ativo;
    }

    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public int getDescontoPercentual() { return descontoPercentual; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public boolean isTodosServicos() { return todosServicos; }
    public boolean isAtivo() { return ativo; }
    public Set<Servico> getServicos() { return servicos; }
}
