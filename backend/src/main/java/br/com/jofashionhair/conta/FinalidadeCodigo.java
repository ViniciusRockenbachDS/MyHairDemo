package br.com.jofashionhair.conta;

import java.time.Duration;

/** Para que serve o código enviado por e-mail e por quanto tempo ele vale. */
public enum FinalidadeCodigo {
    REDEFINIR_SENHA(Duration.ofMinutes(15), "redefinir sua senha"),
    TROCAR_SENHA(Duration.ofMinutes(15), "trocar sua senha"),
    CONVITE(Duration.ofHours(48), "criar sua senha de acesso"),
    NOVO_DISPOSITIVO(Duration.ofMinutes(15), "confirmar o acesso em um dispositivo novo");

    private final Duration validade;
    private final String descricao;

    FinalidadeCodigo(Duration validade, String descricao) {
        this.validade = validade;
        this.descricao = descricao;
    }

    public Duration getValidade() { return validade; }
    public String getDescricao() { return descricao; }
}
