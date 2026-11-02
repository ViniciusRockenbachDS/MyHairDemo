package br.com.jofashionhair.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados já coletados na conversa, gravados como JSONB na coluna
 * atendimento_whatsapp_estado.contexto.
 *
 * <p>Datas/horas ficam como texto ISO para o JSON ser legível e estável.
 * Métodos auxiliares NÃO usam prefixo get/set para o Jackson ignorá-los.</p>
 */
public class ContextoConversa {

    private Long servicoId;
    private Long profissionalId;
    private boolean semPreferencia;
    private String data;   // yyyy-MM-dd
    private String hora;   // HH:mm

    /** Menus numerados enviados ao cliente (posição 1 = índice 0). */
    private List<Long> opcoesServicos = new ArrayList<>();
    private List<Long> opcoesProfissionais = new ArrayList<>();   // a opção 1 é sempre "sem preferência"
    private List<OpcaoHorario> opcoesHorarios = new ArrayList<>();
    private List<Long> opcoesAgendamentos = new ArrayList<>();

    /** Agendamento cuja confirmação foi pedida no lembrete (Passo 5). */
    private Long agendamentoLembreteId;

    private int tentativasInvalidas;

    /** Reservado (não usado sem IA); mantido para não quebrar conversas já gravadas. */
    private List<MensagemIa> historicoIa = new ArrayList<>();

    /** Última mensagem da cliente (aparece no painel em "Conversas"). */
    private String ultimaMensagemCliente;

    // ------------------------------------------------------------------ auxiliares

    public LocalDate dataLocal() {
        return data == null ? null : LocalDate.parse(data);
    }

    public LocalTime horaLocal() {
        return hora == null ? null : LocalTime.parse(hora);
    }

    public void definirData(LocalDate valor) {
        this.data = valor == null ? null : valor.toString();
    }

    public void definirHora(LocalTime valor) {
        this.hora = valor == null ? null : valor.toString();
    }

    public boolean profissionalDecidido() {
        return semPreferencia || profissionalId != null;
    }

    /** Trocar de serviço invalida a escolha de profissional. */
    public void escolherServico(Long id) {
        this.servicoId = id;
        this.profissionalId = null;
        this.semPreferencia = false;
        this.opcoesProfissionais = new ArrayList<>();
    }

    public void limparServico() {
        escolherServico(null);
    }

    public void escolherProfissional(Long id) {
        this.profissionalId = id;
        this.semPreferencia = false;
    }

    public void escolherSemPreferencia() {
        this.profissionalId = null;
        this.semPreferencia = true;
    }

    // ------------------------------------------------------------------ getters/setters (Jackson)

    public Long getServicoId() { return servicoId; }
    public void setServicoId(Long servicoId) { this.servicoId = servicoId; }
    public Long getProfissionalId() { return profissionalId; }
    public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
    public boolean isSemPreferencia() { return semPreferencia; }
    public void setSemPreferencia(boolean semPreferencia) { this.semPreferencia = semPreferencia; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public List<Long> getOpcoesServicos() { return opcoesServicos; }
    public void setOpcoesServicos(List<Long> v) { this.opcoesServicos = v == null ? new ArrayList<>() : new ArrayList<>(v); }
    public List<Long> getOpcoesProfissionais() { return opcoesProfissionais; }
    public void setOpcoesProfissionais(List<Long> v) { this.opcoesProfissionais = v == null ? new ArrayList<>() : new ArrayList<>(v); }
    public List<OpcaoHorario> getOpcoesHorarios() { return opcoesHorarios; }
    public void setOpcoesHorarios(List<OpcaoHorario> v) { this.opcoesHorarios = v == null ? new ArrayList<>() : new ArrayList<>(v); }
    public List<Long> getOpcoesAgendamentos() { return opcoesAgendamentos; }
    public void setOpcoesAgendamentos(List<Long> v) { this.opcoesAgendamentos = v == null ? new ArrayList<>() : new ArrayList<>(v); }
    public Long getAgendamentoLembreteId() { return agendamentoLembreteId; }
    public void setAgendamentoLembreteId(Long agendamentoLembreteId) { this.agendamentoLembreteId = agendamentoLembreteId; }
    public int getTentativasInvalidas() { return tentativasInvalidas; }
    public void setTentativasInvalidas(int tentativasInvalidas) { this.tentativasInvalidas = tentativasInvalidas; }

    public List<MensagemIa> getHistoricoIa() { return historicoIa; }
    public void setHistoricoIa(List<MensagemIa> v) { this.historicoIa = v == null ? new ArrayList<>() : new ArrayList<>(v); }
    public String getUltimaMensagemCliente() { return ultimaMensagemCliente; }
    public void setUltimaMensagemCliente(String v) { this.ultimaMensagemCliente = v; }

    /** Uma mensagem trocada com a IA: papel "user" (cliente) ou "assistant" (robô). */
    public static class MensagemIa {

        private String papel;
        private String texto;

        public MensagemIa() {
        }

        public MensagemIa(String papel, String texto) {
            this.papel = papel;
            this.texto = texto;
        }

        public String getPapel() { return papel; }
        public void setPapel(String papel) { this.papel = papel; }
        public String getTexto() { return texto; }
        public void setTexto(String texto) { this.texto = texto; }
    }

    /** Um horário oferecido em lista numerada. */
    public static class OpcaoHorario {

        private Long profissionalId;
        private String inicio;   // yyyy-MM-ddTHH:mm

        public OpcaoHorario() {
        }

        public OpcaoHorario(Long profissionalId, LocalDateTime inicio) {
            this.profissionalId = profissionalId;
            this.inicio = inicio.toString();
        }

        public LocalDateTime inicioLocal() {
            return LocalDateTime.parse(inicio);
        }

        public Long getProfissionalId() { return profissionalId; }
        public void setProfissionalId(Long profissionalId) { this.profissionalId = profissionalId; }
        public String getInicio() { return inicio; }
        public void setInicio(String inicio) { this.inicio = inicio; }
    }
}
