package br.com.jofashionhair.service;

import java.time.LocalTime;
import java.util.List;

/** Horários livres de uma profissional em um dia, para um serviço. */
public record HorariosProfissional(Long profissionalId, String profissionalNome, List<LocalTime> horarios) {
}
