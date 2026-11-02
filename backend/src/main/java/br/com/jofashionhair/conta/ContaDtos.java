package br.com.jofashionhair.conta;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contratos da API de contas e acessos. */
public final class ContaDtos {

    private ContaDtos() {
    }

    public record UsuarioResponse(Long id, String nome, String email, PapelUsuario papel, boolean ativo) {
        public static UsuarioResponse de(UsuarioAdmin u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), u.isAtivo());
        }
    }

    public record EsqueciSenhaRequest(@NotBlank @Email(message = "E-mail inválido.") String email) {
    }

    public record RedefinirSenhaRequest(
            @NotBlank @Email(message = "E-mail inválido.") String email,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "O código tem 6 números.") String codigo,
            @NotBlank String novaSenha) {
    }

    public record TrocarSenhaRequest(
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "O código tem 6 números.") String codigo,
            @NotBlank String novaSenha) {
    }

    public record ConviteRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 120) String nome,
            @NotBlank @Email(message = "E-mail inválido.") @Size(max = 160) String email,
            @NotNull PapelUsuario papel) {
    }

    public record AtivoRequest(@NotNull Boolean ativo) {
    }

    public record Mensagem(String mensagem) {
    }
}
