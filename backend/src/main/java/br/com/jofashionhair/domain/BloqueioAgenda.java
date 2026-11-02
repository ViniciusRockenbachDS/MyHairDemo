package br.com.jofashionhair.domain;

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

/** Bloqueio manual de agenda (folga, curso, consulta médica...). */
@Entity
@Table(name = "bloqueios_agenda")
public class BloqueioAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id")
    private Profissional profissional;

    @Column(name = "data_inicio", nullable = false)
    private OffsetDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private OffsetDateTime dataFim;

    @Column(length = 200)
    private String motivo;

    protected BloqueioAgenda() {
    }

    public BloqueioAgenda(Profissional profissional, OffsetDateTime dataInicio, OffsetDateTime dataFim, String motivo) {
        this.profissional = profissional;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public Profissional getProfissional() { return profissional; }
    public OffsetDateTime getDataInicio() { return dataInicio; }
    public OffsetDateTime getDataFim() { return dataFim; }
    public String getMotivo() { return motivo; }
}
