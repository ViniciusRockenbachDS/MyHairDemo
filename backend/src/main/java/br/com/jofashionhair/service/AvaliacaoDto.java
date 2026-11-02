package br.com.jofashionhair.service;

import java.time.LocalDate;

import br.com.jofashionhair.domain.Avaliacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Uma avaliação (entrada e saída da API). */
public record AvaliacaoDto(
        Long id,
        @NotBlank(message = "Informe o nome de quem avaliou.") @Size(max = 120) String autor,
        @NotBlank(message = "Escreva o texto da avaliação.") @Size(max = 3000) String texto,
        @NotNull @Min(1) @Max(5) Integer nota,
        LocalDate data,
        @Size(max = 500) @Pattern(regexp = SalaoInformacoesDto.LINK_IMAGEM, message = "Foto inválida.") String fotoUrl,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.*", message = "O link deve começar com https://") String link,
        Integer ordem,
        Boolean ativo) {

    public static AvaliacaoDto de(Avaliacao a) {
        return new AvaliacaoDto(a.getId(), a.getAutor(), a.getTexto(), a.getNota().intValue(), a.getData(),
                a.getFotoUrl(), a.getLink(), a.getOrdem(), a.isAtivo());
    }
}
