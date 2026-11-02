package br.com.jofashionhair.web.publico;

import br.com.jofashionhair.domain.Profissional;

/** Profissional na etapa "Escolha a profissional" do agendamento. */
public record ProfissionalPublicoResponse(Long id, String nome, String especialidade, String fotoUrl) {

    public static ProfissionalPublicoResponse de(Profissional p) {
        return new ProfissionalPublicoResponse(p.getId(), p.getNome(), p.getEspecialidade(), p.getFotoUrl());
    }
}
