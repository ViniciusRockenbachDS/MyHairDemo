package br.com.jofashionhair.domain;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Horário de funcionamento do salão em um dia da semana (1 = segunda ... 7 = domingo). */
@Entity
@Table(name = "horario_funcionamento")
public class HorarioFuncionamento {

    @Id
    @Column(name = "dia_semana")
    private Short diaSemana;

    @Column(nullable = false)
    private boolean fechado;

    @Column(name = "hora_abertura")
    private LocalTime horaAbertura;

    @Column(name = "hora_fechamento")
    private LocalTime horaFechamento;

    protected HorarioFuncionamento() {
    }

    public HorarioFuncionamento(int diaSemana) {
        this.diaSemana = (short) diaSemana;
        this.fechado = true;
    }

    public void atualizar(boolean fechado, LocalTime abertura, LocalTime fechamento) {
        this.fechado = fechado;
        this.horaAbertura = fechado ? null : abertura;
        this.horaFechamento = fechado ? null : fechamento;
    }

    /** true se o salão está aberto durante TODO o intervalo [inicio, fim]. */
    public boolean cobre(LocalTime inicio, LocalTime fim) {
        return !fechado && horaAbertura != null && horaFechamento != null
                && !inicio.isBefore(horaAbertura) && !fim.isAfter(horaFechamento);
    }

    public Short getDiaSemana() { return diaSemana; }
    public boolean isFechado() { return fechado; }
    public LocalTime getHoraAbertura() { return horaAbertura; }
    public LocalTime getHoraFechamento() { return horaFechamento; }
}
