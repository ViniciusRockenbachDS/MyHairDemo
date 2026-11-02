package br.com.jofashionhair.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import br.com.jofashionhair.config.AppProperties;

/**
 * Centraliza "agora" e as conversões de fuso. Regra da casa:
 * o cliente e o expediente falam em horário LOCAL (LocalDateTime),
 * o banco guarda INSTANTES (OffsetDateTime / TIMESTAMPTZ).
 */
@Component
public class RelogioSalao {

    private final Clock clock;
    private final ZoneId zona;

    public RelogioSalao(Clock clock, AppProperties props) {
        this.clock = clock;
        this.zona = props.zoneId();
    }

    public LocalDateTime agora() {
        return LocalDateTime.now(clock.withZone(zona));
    }

    public OffsetDateTime agoraOffset() {
        return OffsetDateTime.now(clock.withZone(zona));
    }

    public OffsetDateTime paraOffset(LocalDateTime local) {
        return local.atZone(zona).toOffsetDateTime();
    }

    public LocalDateTime paraLocal(OffsetDateTime instante) {
        return instante.atZoneSameInstant(zona).toLocalDateTime();
    }

    public ZoneId zona() {
        return zona;
    }
}
