package br.com.jofashionhair.service;

import java.util.List;

/** Profissional como aparece na seção "Equipe" do site. */
public record MembroEquipe(Long id, String nome, String especialidade, String bio, String fotoUrl,
                           List<String> servicos) {
}
