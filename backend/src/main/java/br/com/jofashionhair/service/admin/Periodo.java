package br.com.jofashionhair.service.admin;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import br.com.jofashionhair.exception.RegraNegocioException;

/** Validação comum dos filtros "de/até" das telas do /admin. */
final class Periodo {

    static final int MAX_DIAS = 62;

    private Periodo() {
    }

    static void validar(LocalDate de, LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new RegraNegocioException("A data final deve ser igual ou posterior à inicial.");
        }
        if (ChronoUnit.DAYS.between(de, ate) > MAX_DIAS) {
            throw new RegraNegocioException("Consulte no máximo " + MAX_DIAS + " dias por vez.");
        }
    }
}
