package br.com.jofashionhair.service;

import java.time.LocalTime;

import br.com.jofashionhair.domain.HorarioFuncionamento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Horário de funcionamento de um dia (1 = segunda ... 7 = domingo). */
public record HorarioFuncionamentoDto(
        @NotNull @Min(1) @Max(7) Integer diaSemana,
        boolean fechado,
        LocalTime abertura,
        LocalTime fechamento) {

    public static HorarioFuncionamentoDto de(HorarioFuncionamento h) {
        return new HorarioFuncionamentoDto(h.getDiaSemana().intValue(), h.isFechado(),
                h.getHoraAbertura(), h.getHoraFechamento());
    }
}
