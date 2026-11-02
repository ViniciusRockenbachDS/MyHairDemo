package br.com.jofashionhair.whatsapp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import br.com.jofashionhair.domain.AtendimentoWhatsappEstado;
import br.com.jofashionhair.domain.EstadoConversa;
import br.com.jofashionhair.domain.Telefone;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.AtendimentoWhatsappEstadoRepository;
import br.com.jofashionhair.repository.MensagemProcessadaRepository;
import br.com.jofashionhair.service.RelogioSalao;

/**
 * Orquestra o atendimento: idempotência → trava por telefone → carrega estado →
 * máquina de estados → grava estado → envia a resposta.
 *
 * <p>A trava garante que duas mensagens seguidas do MESMO cliente sejam
 * processadas uma depois da outra (sem perder estado). Clientes diferentes
 * são atendidos em paralelo. As travas são em memória: com várias instâncias
 * da aplicação, troque por pg_advisory_xact_lock.</p>
 */
@Service
public class AtendimentoWhatsappService {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoWhatsappService.class);
    private static final int QTD_TRAVAS = 64;

    private final ReentrantLock[] travas = new ReentrantLock[QTD_TRAVAS];

    private final AtendimentoWhatsappEstadoRepository estadoRepository;
    private final MensagemProcessadaRepository mensagemRepository;
    private final MaquinaEstadosConversa maquina;
    private final EvolutionApiClient evolution;
    private final RelogioSalao relogio;

    public AtendimentoWhatsappService(AtendimentoWhatsappEstadoRepository estadoRepository,
                                      MensagemProcessadaRepository mensagemRepository,
                                      MaquinaEstadosConversa maquina,
                                      EvolutionApiClient evolution,
                                      RelogioSalao relogio) {
        this.estadoRepository = estadoRepository;
        this.mensagemRepository = mensagemRepository;
        this.maquina = maquina;
        this.evolution = evolution;
        this.relogio = relogio;
        for (int i = 0; i < QTD_TRAVAS; i++) {
            travas[i] = new ReentrantLock();
        }
    }

    /** Chamado pelo webhook. Nunca lança exceção (o webhook sempre responde 200). */
    public void processarMensagemRecebida(MensagemRecebida mensagem) {
        try {
            if (mensagem.messageId() != null && mensagemRepository.registrar(mensagem.messageId()) == 0) {
                log.debug("Mensagem {} repetida, ignorada", mensagem.messageId());
                return;
            }
            String resposta = processarTexto(mensagem.numero(), mensagem.nomeContato(), mensagem.texto());
            if (resposta != null && !resposta.isBlank()) {
                evolution.enviarTexto(mensagem.numero(), resposta);
            }
        } catch (RuntimeException e) {
            log.error("Erro ao processar mensagem do WhatsApp de {}", mensagem.numero(), e);
        }
    }

    /**
     * Processa um texto e devolve a resposta SEM enviar (o simulador do /admin usa direto).
     * @return resposta, ou null quando o robô não deve responder (atendimento humano)
     */
    public String processarTexto(String numero, String nomeContato, String texto) {
        String telefone = Telefone.normalizar(numero);
        ReentrantLock trava = travas[Math.floorMod(telefone.hashCode(), QTD_TRAVAS)];
        trava.lock();
        try {
            AtendimentoWhatsappEstado conversa = estadoRepository.findByTelefone(telefone)
                    .orElseGet(() -> new AtendimentoWhatsappEstado(telefone));
            String resposta;
            try {
                resposta = maquina.processar(conversa, nomeContato, texto);
            } catch (RuntimeException e) {
                log.error("Erro na conversa de {}", telefone, e);
                conversa.reiniciar();
                resposta = MensagensBot.ERRO_INTERNO;
            }
            if (texto != null && !texto.isBlank()) {
                conversa.getContexto().setUltimaMensagemCliente(texto.length() > 300 ? texto.substring(0, 300) + "…" : texto);
            }
            estadoRepository.save(conversa);
            return resposta;
        } finally {
            trava.unlock();
        }
    }

    /**
     * Deixa a conversa aguardando "1 - Confirmar / 2 - Cancelar" de um lembrete.
     * Usado pela rotina de lembretes (Passo 5).
     */
    public void aguardarConfirmacaoLembrete(String telefoneCliente, Long agendamentoId, OffsetDateTime ate) {
        String telefone = Telefone.normalizar(telefoneCliente);
        ReentrantLock trava = travas[Math.floorMod(telefone.hashCode(), QTD_TRAVAS)];
        trava.lock();
        try {
            AtendimentoWhatsappEstado conversa = estadoRepository.findByTelefone(telefone)
                    .orElseGet(() -> new AtendimentoWhatsappEstado(telefone));
            if (conversa.getEstado() == EstadoConversa.ATENDIMENTO_HUMANO && !conversa.expirada(relogio.agoraOffset())) {
                return;   // não atropela um atendimento humano em andamento
            }
            conversa.aguardarConfirmacaoLembrete(agendamentoId, ate);
            estadoRepository.save(conversa);
        } finally {
            trava.unlock();
        }
    }

    // ------------------------------------------------------------------ /admin

    public List<ConversaResumo> listarConversas(EstadoConversa estado) {
        List<AtendimentoWhatsappEstado> lista = estado == null
                ? estadoRepository.findTop100ByOrderByUltimaInteracaoEmDesc()
                : estadoRepository.findByEstadoOrderByUltimaInteracaoEmDesc(estado);
        return lista.stream()
                .map(c -> new ConversaResumo(c.getTelefone(), c.getEstado(),
                        relogio.paraLocal(c.getUltimaInteracaoEm()),
                        c.getExpiraEm() == null ? null : relogio.paraLocal(c.getExpiraEm()),
                        c.getContexto() == null ? null : c.getContexto().getUltimaMensagemCliente()))
                .toList();
    }

    /** Devolve a conversa ao robô (ex.: depois que a equipe terminou o atendimento humano). */
    public void reativarRobo(String telefoneBruto) {
        String telefone = Telefone.normalizar(telefoneBruto);
        ReentrantLock trava = travas[Math.floorMod(telefone.hashCode(), QTD_TRAVAS)];
        trava.lock();
        try {
            AtendimentoWhatsappEstado conversa = estadoRepository.findByTelefone(telefone)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada."));
            conversa.reiniciar();
            estadoRepository.save(conversa);
        } finally {
            trava.unlock();
        }
    }

    /** Todo dia às 3h30 apaga ids de mensagens com mais de 7 dias. */
    @Scheduled(cron = "0 30 3 * * *", zone = "America/Sao_Paulo")
    public void limparMensagensAntigas() {
        int apagadas = mensagemRepository.apagarAntesDe(relogio.agoraOffset().minusDays(7));
        log.info("Limpeza do webhook: {} ids de mensagens antigas removidos", apagadas);
    }
}
