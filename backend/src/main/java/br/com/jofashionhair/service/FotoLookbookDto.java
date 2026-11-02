package br.com.jofashionhair.service;

import br.com.jofashionhair.domain.FotoLookbook;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Foto do Lookbook (entrada e saída da API). */
public record FotoLookbookDto(
        Long id,
        @NotBlank(message = "Escolha a foto.") @Size(max = 500)
        @Pattern(regexp = SalaoInformacoesDto.LINK_IMAGEM, message = "Foto inválida.") String fotoUrl,
        @Size(max = 120) String legenda,
        @Min(0) Integer ordem,
        Boolean ativo) {

    public static FotoLookbookDto de(FotoLookbook f) {
        return new FotoLookbookDto(f.getId(), f.getFotoUrl(), f.getLegenda(), f.getOrdem(), f.isAtivo());
    }
}
