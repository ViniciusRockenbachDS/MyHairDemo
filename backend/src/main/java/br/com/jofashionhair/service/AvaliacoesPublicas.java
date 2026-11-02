package br.com.jofashionhair.service;

import java.math.BigDecimal;
import java.util.List;

/** O que o site recebe: resumo + avaliações visíveis. Sem avaliações, o site esconde a seção. */
public record AvaliacoesPublicas(BigDecimal nota, Integer total, String link, List<AvaliacaoDto> avaliacoes) {
}
