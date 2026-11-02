package br.com.jofashionhair.privacidade;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.jofashionhair.service.AgendamentoResumo;

/** Tudo o que o sistema guarda sobre uma cliente (atende pedidos de acesso da LGPD). */
public record ExportacaoCliente(
        OffsetDateTime geradoEm,
        Long clienteId,
        String nome,
        String telefone,
        String email,
        String observacoes,
        List<AgendamentoResumo> agendamentos) {
}
