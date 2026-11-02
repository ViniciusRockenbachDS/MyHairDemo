package br.com.jofashionhair.service;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Selo do topo da seção: "4,9 · 78 avaliações" + link "Ver todas". */
public record ResumoAvaliacoesDto(
        @DecimalMin(value = "1.0", message = "A nota vai de 1 a 5.")
        @DecimalMax(value = "5.0", message = "A nota vai de 1 a 5.")
        @Digits(integer = 1, fraction = 1, message = "Use uma casa decimal, ex.: 4,9.")
        BigDecimal nota,
        @Min(0) Integer total,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.*", message = "O link deve começar com https://") String link) {
}
