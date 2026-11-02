package br.com.jofashionhair.service;

/**
 * Publicado sempre que algo que o site exibe muda (serviços, profissionais,
 * expediente, bloqueios ou agendamentos). É repassado ao navegador via SSE
 * DEPOIS do commit, para o site nunca recarregar dados ainda não gravados.
 */
public record CatalogoAlteradoEvent(String tipo) {

    public static final String SERVICOS = "servicos";
    public static final String PROFISSIONAIS = "profissionais";
    public static final String AGENDA = "agenda";
    public static final String SALAO = "salao";
    public static final String AVALIACOES = "avaliacoes";
    public static final String PRODUTOS = "produtos";
    public static final String PROMOCOES = "promocoes";
    public static final String LOOKBOOK = "lookbook";
}
