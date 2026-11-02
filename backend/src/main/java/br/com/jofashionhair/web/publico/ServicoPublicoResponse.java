package br.com.jofashionhair.web.publico;

import java.math.BigDecimal;

import br.com.jofashionhair.domain.Servico;

public record ServicoPublicoResponse(
        Long id,
        String nome,
        String descricao,
        String categoria,
        BigDecimal preco,
        Integer duracaoMinutos,
        String fotoUrl,
        boolean exigeSinal,
        BigDecimal valorSinal) {

    public static ServicoPublicoResponse de(Servico s) {
        return new ServicoPublicoResponse(s.getId(), s.getNome(), s.getDescricao(),
                s.getCategoria(), s.getPreco(), s.getDuracaoMinutos(), s.getFotoUrl(),
                s.isExigeSinal(), s.getValorSinal());
    }
}
