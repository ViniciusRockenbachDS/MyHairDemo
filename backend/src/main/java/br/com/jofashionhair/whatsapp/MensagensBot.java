package br.com.jofashionhair.whatsapp;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import br.com.jofashionhair.config.AppProperties;
import br.com.jofashionhair.domain.Disponibilidade;
import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.SalaoInformacoesDto;
import br.com.jofashionhair.service.SalaoService;
import br.com.jofashionhair.service.SugestaoHorario;

/**
 * Todos os textos do robô num só lugar (fácil de ajustar o tom da marca).
 * *texto* = negrito no WhatsApp.
 */
@Component
public class MensagensBot {

    static final String ERRO_INTERNO = "Desculpe, tive um probleminha aqui 😕. Pode tentar de novo em instantes? "
            + "Se preferir, digite *atendente* para falar com a equipe.";

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA_LONGA = DateTimeFormatter.ofPattern("EEEE, dd/MM", PT_BR);
    private static final DateTimeFormatter DATA_CURTA = DateTimeFormatter.ofPattern("EEE dd/MM", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String RODAPE_HORARIOS = "\n\nResponda com o *número* ou me diga outro dia/horário.";

    private final SalaoService salaoService;
    private final String linkPrivacidade;

    /** O nome do salão é lido do banco: se o dono mudar no /admin, o robô já usa o novo. */
    public MensagensBot(SalaoService salaoService, AppProperties props) {
        this.salaoService = salaoService;
        String site = props.siteUrl() == null ? "" : props.siteUrl().replaceAll("/+$", "");
        this.linkPrivacidade = site + "/privacidade";
    }

    private String nomeSalao() {
        return salaoService.informacoes().nome();
    }

    // ------------------------------------------------------------------ fluxo de agendamento

    /** Primeira mensagem de cada conversa (formato definido pelo salão). */
    public String saudacao(String nomeContato) {
        String primeiro = primeiroNome(nomeContato);
        return "Olá" + (primeiro.isEmpty() ? "" : " *" + primeiro + "*") + "!! 😊\n\n"
                + "Seja Bem Vindo (a) ao *" + nomeSalao() + "* ✂️\n\n"
                + "Será um prazer atendê-lo (a):\n\n"
                + "❔Informe qual serviço deseja;\n"
                + "💰A tabela de valores é fixa e você pode solicitá-la.\n\n"
                + "Se houver dúvida pode fazer ⭐\n\n"
                + "_Seus dados são usados só para o atendimento: " + linkPrivacidade + "_\n\n";
    }

    /** Tabela de valores atual (a mesma do site), numerada para a cliente escolher. */
    public String tabelaValores(List<Servico> servicos) {
        return tabelaValores(servicos, null);
    }

    /** @param promocoes texto das promoções ativas (null se não houver) */
    public String tabelaValores(List<Servico> servicos, String promocoes) {
        StringBuilder sb = new StringBuilder("💰 *Tabela de valores*\n\n");
        for (int i = 0; i < servicos.size(); i++) {
            Servico s = servicos.get(i);
            sb.append(i + 1).append(" - ").append(s.getNome())
                    .append(" • ").append(moeda(s.getPreco()))
                    .append(" • ").append(duracao(s.getDuracaoMinutos())).append('\n');
        }
        if (promocoes != null) {
            sb.append('\n').append(promocoes).append('\n');
        }
        return sb.append("\nQual serviço deseja? Responda com o *número* ou o *nome*.").toString();
    }

    /** Fora do horário: o robô continua agendando, mas não chama uma pessoa. */
    public String fechadoAgora(String proximaAbertura) {
        return "No momento estamos fechados 😴"
                + (proximaAbertura != null ? " Abrimos " + proximaAbertura + "." : "")
                + "\n\nMas você pode agendar por aqui normalmente: é só me dizer o serviço, o dia e o horário ✂️";
    }

    public String saiuPromocoes() {
        return "Pronto! Você não vai mais receber promoções por aqui 👍\n"
                + "Os lembretes dos seus horários continuam normalmente.";
    }

    /** Quando o robô não sabe responder: a conversa vai para "Pediram atendente". */
    public String encaminhadoAtendente() {
        return "Alguém já irá te atender 😊";
    }

    public String perguntarServico(List<Servico> servicos, boolean filtrado) {
        StringBuilder sb = new StringBuilder(filtrado
                ? "Encontrei mais de um serviço parecido. Qual deles você deseja?"
                : "Qual serviço você deseja realizar?");
        sb.append("\n\n");
        for (int i = 0; i < servicos.size(); i++) {
            Servico s = servicos.get(i);
            sb.append(i + 1).append(" - ").append(s.getNome())
                    .append(" • ").append(duracao(s.getDuracaoMinutos()))
                    .append(" • ").append(moeda(s.getPreco())).append('\n');
        }
        return sb.append("\nResponda com o *número* ou o *nome* do serviço.").toString();
    }

    public String perguntarProfissional(Servico servico, List<Profissional> profissionais) {
        StringBuilder sb = new StringBuilder("Perfeito! *").append(servico.getNome()).append("* (")
                .append(duracao(servico.getDuracaoMinutos())).append(" • ").append(moeda(servico.getPreco()))
                .append(")\n\nVocê tem preferência por alguma profissional?\n\n1 - Sem preferência\n");
        for (int i = 0; i < profissionais.size(); i++) {
            Profissional p = profissionais.get(i);
            sb.append(i + 2).append(" - ").append(p.getNome());
            if (p.getEspecialidade() != null && !p.getEspecialidade().isBlank()) {
                sb.append(" (").append(p.getEspecialidade()).append(')');
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    public String perguntarDataHora(Servico servico, LocalTime horaJaInformada) {
        if (horaJaInformada != null) {
            return "Para qual *dia* você gostaria de agendar *" + servico.getNome() + "* às "
                    + HORA.format(horaJaInformada) + "? (ex.: *sexta* ou *16/10*)";
        }
        return "Para qual *dia e horário* você gostaria de agendar *" + servico.getNome()
                + "*? (ex.: *sexta às 17h* ou *16/10 às 14:30*)";
    }

    public String horariosDoDia(LocalDate data, List<SugestaoHorario> opcoes) {
        return "Estes são os horários livres em *" + DATA_LONGA.format(data) + "*:\n\n"
                + listar(opcoes, false) + RODAPE_HORARIOS;
    }

    public String semHorariosNoDia(LocalDate data, List<SugestaoHorario> sugestoes) {
        return "Não tenho horários livres em *" + DATA_LONGA.format(data) + "* 😕\n\nPosso te oferecer:\n\n"
                + listar(sugestoes, true) + RODAPE_HORARIOS;
    }

    public String semHorariosProximos(LocalDate data) {
        return "Não encontrei horários livres a partir de *" + DATA_LONGA.format(data)
                + "* nos próximos dias 😕. Me diga outra data ou digite *atendente* para falar com a equipe.";
    }

    public String indisponivel(Disponibilidade motivo, List<SugestaoHorario> sugestoes) {
        return "😕 " + motivo.getMensagem() + "\n\nPosso te oferecer:\n\n" + listar(sugestoes, true) + RODAPE_HORARIOS;
    }

    public String indisponivelSemSugestoes(Disponibilidade motivo) {
        return "😕 " + motivo.getMensagem() + "\n\nNão encontrei horários livres nos próximos dias. "
                + "Me diga outra data ou digite *atendente* para falar com a equipe.";
    }

    public String confirmado(AgendamentoResumo r) {
        return "✅ *Agendamento confirmado!*\n\n"
                + "💅 " + r.servico() + "\n"
                + "👩 " + r.profissional() + "\n"
                + "📅 " + capitalizar(DATA_LONGA.format(r.inicio())) + " às " + HORA.format(r.inicio())
                + " (até " + HORA.format(r.fim()) + ")\n"
                + "💰 " + moeda(r.preco()) + "\n\n"
                + "Vou te mandar um lembrete antes do horário. Se precisar cancelar, é só enviar *cancelar*. "
                + "Até lá! 💖";
    }

    // ------------------------------------------------------------------ sinal por Pix

    /** Resposta do robô logo após reservar um serviço com sinal. */
    public String sinalPendente(AgendamentoResumo r, br.com.jofashionhair.pagamento.SinalService.SinalInfo s,
                                long minutosParaPagar) {
        return "📌 *Quase lá!* Para garantir seu horário, pague o sinal de *" + moeda(s.valor()) + "* por Pix em até "
                + minutosParaPagar + " minutos.\n\n"
                + "💅 " + r.servico() + "\n"
                + "👩 " + r.profissional() + "\n"
                + "📅 " + capitalizar(DATA_LONGA.format(r.inicio())) + " às " + HORA.format(r.inicio()) + "\n\n"
                + "*Pix copia e cola* (copie o código abaixo e cole no app do seu banco):\n\n"
                + s.copiaCola() + "\n\n"
                + "💡 O sinal é descontado do valor do serviço: no dia, você paga só *" + moeda(s.restante()) + "*.\n\n"
                + (s.automatico()
                        ? "Assim que o pagamento cair, eu confirmo seu horário por aqui ✅"
                        : "Depois de pagar, envie o comprovante aqui. Assim que a equipe conferir, seu horário fica confirmado ✅");
    }

    public String sinalRecebido(AgendamentoResumo r) {
        BigDecimal restante = r.valorSinal() == null ? r.preco() : r.preco().subtract(r.valorSinal());
        return "✅ *Sinal recebido! Seu horário está confirmado.*\n\n"
                + "💅 " + r.servico() + "\n"
                + "👩 " + r.profissional() + "\n"
                + "📅 " + capitalizar(DATA_LONGA.format(r.inicio())) + " às " + HORA.format(r.inicio()) + "\n\n"
                + "💰 Sinal pago: " + moeda(r.valorSinal()) + ". O sinal é descontado do valor do serviço: "
                + "no dia, falta pagar *" + moeda(restante) + "*.\n\n"
                + "Vou te mandar um lembrete antes do horário. Até lá! 💖";
    }

    public String sinalExpirado(AgendamentoResumo r) {
        return "⏰ O prazo para pagar o sinal de *" + r.servico() + "* ("
                + capitalizar(DATA_LONGA.format(r.inicio())) + " às " + HORA.format(r.inicio())
                + ") terminou e o horário foi liberado.\n\n"
                + "Se ainda quiser, é só agendar de novo por aqui ou pelo site 😊";
    }

    /** Cliente avisou que pagou. */
    public String pagamentoEmConferencia(boolean automatico) {
        return automatico
                ? "Obrigada! Assim que o Pix for confirmado, eu te aviso por aqui ✅"
                : "Obrigada! A equipe vai conferir o Pix e eu te aviso assim que o horário for confirmado ✅";
    }

    // ------------------------------------------------------------------ lembretes automáticos

    /**
     * Lembrete enviado pela rotina (24h / 2h antes).
     * @param pedirConfirmacao true = termina com "1 - Confirmar / 2 - Cancelar"
     */
    public String lembrete(AgendamentoResumo r, boolean pedirConfirmacao, LocalDate hoje) {
        SalaoInformacoesDto salao = salaoService.informacoes();
        String primeiro = r.cliente() == null || r.cliente().startsWith("Cliente WhatsApp") ? "" : primeiroNome(r.cliente());
        LocalDate dia = r.inicio().toLocalDate();
        String quando = dia.equals(hoje) ? "Hoje"
                : dia.equals(hoje.plusDays(1)) ? "Amanhã (" + DATA_LONGA.format(r.inicio()) + ")"
                : capitalizar(DATA_LONGA.format(r.inicio()));

        StringBuilder sb = new StringBuilder("Olá").append(primeiro.isEmpty() ? "" : ", " + primeiro)
                .append("! 💇‍♀️ Passando para lembrar do seu horário no *").append(salao.nome()).append("*:\n\n")
                .append("💅 ").append(r.servico()).append('\n')
                .append("👩 ").append(r.profissional()).append('\n')
                .append("📅 *").append(quando).append(" às ").append(HORA.format(r.inicio())).append("*\n");
        if (salao.endereco() != null) {
            sb.append("📍 ").append(salao.endereco()).append('\n');
        }
        if (pedirConfirmacao) {
            sb.append("\nVocê confirma sua presença?\n\n1 - Confirmar ✅\n2 - Cancelar ❌");
        } else {
            sb.append("\nTe esperamos! Se precisar cancelar, é só enviar *cancelar*. 💖");
        }
        return sb.toString();
    }

    /** Enviada quando a cliente agenda pelo SITE (o agendamento nasce PENDENTE). */
    public String recebidoPeloSite(AgendamentoResumo r) {
        return "✅ *Recebemos seu agendamento no " + nomeSalao() + "!*\n\n"
                + "💅 " + r.servico() + "\n"
                + "👩 " + r.profissional() + "\n"
                + "📅 " + capitalizar(DATA_LONGA.format(r.inicio())) + " às " + HORA.format(r.inicio())
                + " (até " + HORA.format(r.fim()) + ")\n"
                + "💰 " + moeda(r.preco()) + "\n\n"
                + "Um dia antes eu te mando um lembrete para confirmar a presença. "
                + "Se precisar cancelar, é só responder *cancelar*. 💖";
    }

    // ------------------------------------------------------------------ cancelamento e lembrete

    public String semAgendamentosFuturos() {
        return "Você não tem agendamentos futuros conosco. Quer marcar um horário? É só me dizer o serviço 😊";
    }

    public String escolherCancelamento(List<AgendamentoResumo> agendamentos) {
        StringBuilder sb = new StringBuilder("Qual agendamento você deseja cancelar?\n\n");
        for (int i = 0; i < agendamentos.size(); i++) {
            AgendamentoResumo a = agendamentos.get(i);
            sb.append(i + 1).append(" - ").append(DATA_CURTA.format(a.inicio())).append(" às ")
                    .append(HORA.format(a.inicio())).append(" • ").append(a.servico())
                    .append(" com ").append(a.profissional()).append('\n');
        }
        return sb.append("\nResponda com o *número* (ou *não* para manter).").toString();
    }

    public String cancelado(AgendamentoResumo r) {
        return "Pronto, seu agendamento de *" + r.servico() + "* em " + DATA_LONGA.format(r.inicio())
                + " às " + HORA.format(r.inicio()) + " foi cancelado.\n\n"
                + "Quer remarcar? É só me dizer o novo dia e horário 😊";
    }

    public String nadaCancelado() {
        return "Tudo certo, nenhum agendamento foi cancelado 😊";
    }

    public String presencaConfirmada(AgendamentoResumo r) {
        return "Obrigada por confirmar! ✅ Te esperamos *" + DATA_LONGA.format(r.inicio()) + " às "
                + HORA.format(r.inicio()) + "*. 💖";
    }

    public String agendamentoInativo() {
        return "Esse agendamento não está mais ativo. Se quiser marcar um novo horário, é só me dizer 😊";
    }

    public String respostaLembreteNaoEntendida() {
        return "Para confirmar sua presença responda *1* (ou *sim*). Para cancelar, responda *2*.";
    }

    // ------------------------------------------------------------------ diversos

    public String atendimentoHumano() {
        return encaminhadoAtendente();
    }

    public String soTexto() {
        return "Por enquanto eu só consigo ler mensagens de texto 😅. Pode escrever, por favor?";
    }

    public String naoEntendi(int tentativas) {
        return "Desculpe, não entendi 😅. "
                + (tentativas >= 3 ? "Se preferir, digite *atendente* para falar com a equipe.\n\n" : "\n\n");
    }

    public String deNada() {
        return "Imagina! Qualquer coisa é só chamar 💖";
    }

    public String servicoIndisponivel() {
        return "Esse serviço não está mais disponível para agendamento online 😕\n\n";
    }

    public String semServicos() {
        return "No momento não há serviços disponíveis para agendamento online. "
                + "Digite *atendente* para falar com a equipe.";
    }

    public String semProfissional(Servico servico) {
        return "No momento nenhuma profissional está atendendo *" + servico.getNome()
                + "* pelo agendamento online 😕\n\n";
    }

    // ------------------------------------------------------------------ formatação

    private String listar(List<SugestaoHorario> opcoes, boolean mostrarData) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < opcoes.size(); i++) {
            SugestaoHorario s = opcoes.get(i);
            LocalDateTime inicio = s.inicio();
            sb.append(i + 1).append(" - ");
            if (mostrarData) {
                sb.append(DATA_CURTA.format(inicio)).append(" às ");
            }
            sb.append(HORA.format(inicio)).append(" com ").append(s.profissionalNome()).append('\n');
        }
        return sb.toString().trim();
    }

    static String duracao(int minutos) {
        if (minutos < 60) {
            return minutos + " min";
        }
        int h = minutos / 60;
        int m = minutos % 60;
        return m == 0 ? h + "h" : h + "h" + String.format("%02d", m);
    }

    static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        String primeiro = nome.trim().split("\\s+")[0];
        return primeiro.length() > 30 ? primeiro.substring(0, 30) : primeiro;
    }
}
