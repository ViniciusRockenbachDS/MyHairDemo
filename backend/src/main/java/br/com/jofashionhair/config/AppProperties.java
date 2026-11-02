package br.com.jofashionhair.config;

import java.time.ZoneId;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades tipadas do prefixo {@code app} do application.yml.
 * (Nome, endereço e contatos do salão NÃO ficam aqui: são editados no /admin.)
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        String siteUrl,
        Admin admin,
        Email email,
        Seguranca seguranca,
        Whatsapp whatsapp,
        Pagamentos pagamentos,
        Lembretes lembretes,
        Uploads uploads,
        Agenda agenda,
        Cors cors,
        Demo demo) {

    /**
     * Conta do DONO criada na primeira vez que a API sobe.
     * @param forcarSenha emergência: redefine a senha do dono para {@code senha} ao iniciar
     */
    public record Admin(String email, String nome, String senha, boolean forcarSenha) {
    }

    /**
     * @param cookieSeguro              cookie do dispositivo só trafega por HTTPS (desligar só em localhost)
     * @param diasDispositivoConfiavel  quanto tempo um dispositivo confirmado dispensa o código
     */
    public record Seguranca(boolean cookieSeguro, int diasDispositivoConfiavel) {
    }

    /** @param enviar false = o e-mail só aparece no log (desenvolvimento) */
    public record Email(boolean enviar, String remetente) {
    }

    /**
     * @param enviarMensagens             false = só registra no log (útil sem Evolution configurada)
     * @param expiracaoConversaHoras      conversa parada há mais que isso recomeça do zero
     * @param pausaAtendimentoHumanoHoras tempo que o robô fica em silêncio após "atendente"
     */
    public record Whatsapp(
            String baseUrl,
            String apiKey,
            String instance,
            String webhookSecret,
            boolean enviarMensagens,
            int expiracaoConversaHoras,
            int pausaAtendimentoHumanoHoras) {
    }

    /**
     * @param habilitado         liga/desliga a rotina
     * @param primeiroHoras      antecedência do 1º lembrete (padrão 24h)
     * @param segundoHoras       antecedência do 2º lembrete (padrão 2h)
     * @param minimoMinutosAntes não envia lembrete a menos disso do horário
     * @param silencioInicio     "HH:mm" — não envia mensagens a partir deste horário...
     * @param silencioFim        "HH:mm" — ...até este (madrugada)
     */
    /**
     * Sinal por Pix com o Mercado Pago (opcional: sem ele, o Pix é pela chave do salão).
     * @param mercadopagoEmailPagador e-mail exigido pelo Mercado Pago para criar o Pix
     */
    public record Pagamentos(String mercadopagoToken, String mercadopagoWebhookSecret, String mercadopagoEmailPagador) {
    }

    public record Lembretes(
            boolean habilitado,
            int primeiroHoras,
            int segundoHoras,
            int minimoMinutosAntes,
            String silencioInicio,
            String silencioFim) {
    }

    /** @param diretorio pasta onde ficam as imagens enviadas pelo painel */
    public record Uploads(String diretorio) {
    }

    /**
     * Modo demonstração (site público para testar).
     * @param chaveMestra   abre o modo "dono" (edita os dados originais)
     * @param maxSessoes    visitantes ao mesmo tempo
     * @param horasSessao   quanto tempo a cópia de um visitante dura sem uso
     */
    public record Demo(boolean habilitado, String chaveMestra, int maxSessoes, int horasSessao) {
    }

    /**
     * @param slotMinutos               granularidade da grade de horários oferecidos (ex.: 15 min)
     * @param antecedenciaMinimaMinutos antecedência mínima exigida para reservar (site/WhatsApp)
     */
    public record Agenda(int slotMinutos, int antecedenciaMinimaMinutos) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** Fuso horário do salão (America/Sao_Paulo). */
    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }
}
