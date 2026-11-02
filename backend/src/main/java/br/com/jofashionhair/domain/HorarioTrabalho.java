package br.com.jofashionhair.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Uma faixa de expediente (ex.: segunda, 09:00-12:00). */
@Entity
@Table(name = "horario_trabalho")
public class HorarioTrabalho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id")
    private Profissional profissional;

    /** ISO-8601: 1 = segunda ... 7 = domingo (igual a DayOfWeek.getValue()). */
    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    protected HorarioTrabalho() {
    }

    public HorarioTrabalho(Profissional profissional, DayOfWeek dia, LocalTime horaInicio, LocalTime horaFim) {
        this.profissional = profissional;
        this.diaSemana = (short) dia.getValue();
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
    }

    public Long getId() { return id; }
    public Profissional getProfissional() { return profissional; }
    public Short getDiaSemana() { return diaSemana; }
    public DayOfWeek getDayOfWeek() { return DayOfWeek.of(diaSemana); }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
}
