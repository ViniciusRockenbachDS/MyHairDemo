package br.com.jofashionhair.domain;

/**
 * Resultado da verificação de um horário. A mensagem é amigável o bastante
 * para ser enviada direto ao cliente no site ou no WhatsApp.
 */
public enum Disponibilidade {
    DISPONIVEL("Horário disponível."),
    PROFISSIONAL_INATIVO("A profissional escolhida não está atendendo no momento."),
    NAO_REALIZA_SERVICO("A profissional escolhida não realiza este serviço."),
    SALAO_FECHADO("O salão está fechado neste dia ou horário."),
    FORA_DO_EXPEDIENTE("Este horário está fora do expediente da profissional."),
    ANTECEDENCIA_INSUFICIENTE("Este horário já passou ou está muito próximo. Escolha um horário com mais antecedência."),
    BLOQUEADO("A profissional não estará disponível neste período."),
    HORARIO_OCUPADO("Este horário já está reservado.");

    private final String mensagem;

    Disponibilidade(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return mensagem;
    }
}
