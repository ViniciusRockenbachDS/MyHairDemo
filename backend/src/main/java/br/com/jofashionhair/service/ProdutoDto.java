package br.com.jofashionhair.service;

import java.math.BigDecimal;

import br.com.jofashionhair.domain.Produto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Produto (entrada e saída da API). */
public record ProdutoDto(
        Long id,
        @NotBlank(message = "Informe o nome do produto.") @Size(max = 120) String nome,
        @Size(max = 80) String marca,
        @Size(max = 2000) String descricao,
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.") @Digits(integer = 8, fraction = 2)
        BigDecimal preco,
        @Size(max = 500) @Pattern(regexp = SalaoInformacoesDto.LINK_IMAGEM, message = "Foto inválida.") String fotoUrl,
        Boolean disponivel,
        @Min(0) Integer ordem,
        Boolean ativo) {

    public static ProdutoDto de(Produto p) {
        return new ProdutoDto(p.getId(), p.getNome(), p.getMarca(), p.getDescricao(), p.getPreco(), p.getFotoUrl(),
                p.isDisponivel(), p.getOrdem(), p.isAtivo());
    }
}
