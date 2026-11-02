package br.com.jofashionhair.service;

import br.com.jofashionhair.domain.PerguntaFrequente;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pergunta frequente (entrada e saída da API). */
public record PerguntaFrequenteDto(
        Long id,
        @NotBlank(message = "Escreva a pergunta.") @Size(max = 200) String pergunta,
        @NotBlank(message = "Informe ao menos uma palavra-chave.") @Size(max = 1000) String palavrasChave,
        @NotBlank(message = "Escreva a resposta.") @Size(max = 2000) String resposta,
        @Min(0) Integer ordem,
        Boolean ativo) {

    public static PerguntaFrequenteDto de(PerguntaFrequente p) {
        return new PerguntaFrequenteDto(p.getId(), p.getPergunta(), p.getPalavrasChave(), p.getResposta(),
                p.getOrdem(), p.isAtivo());
    }
}
