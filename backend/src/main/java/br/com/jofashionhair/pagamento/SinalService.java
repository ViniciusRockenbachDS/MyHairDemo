package br.com.jofashionhair.pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.jofashionhair.domain.Agendamento;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.CatalogoAlteradoEvent;
import br.com.jofashionhair.service.RelogioSalao;
import br.com.jofashionhair.whatsapp.NotificacaoWhatsapp;

/**
 * Ciclo do sinal: gerar o Pix → confirmar (automático ou pela equipe) →
 * ou liberar o horário quando o prazo acaba.
 */
@Service
public class SinalService {

    private static final Logger log = LoggerFactory.getLogger(SinalService.class);

    /** O que o site e o robô mostram para a cliente pagar. */
    public record SinalInfo(String codigo, BigDecimal valor, BigDecimal restante, String copiaCola,
                            String qrCodeBase64, LocalDateTime expiraEm, boolean automatico) {
    }

    public record StatusSinal(String status, LocalDateTime expiraEm) {
    }

    private final AgendamentoRepository repository;
    private final PixService pixService;
    private final MercadoPagoClient mercadoPago;
    private final NotificacaoWhatsapp notificacao;
    private final ApplicationEventPublisher eventos;
    private final RelogioSalao relogio;

    public SinalService(AgendamentoRepository repository, PixService pixService, MercadoPagoClient mercadoPago,
                        NotificacaoWhatsapp notificacao, ApplicationEventPublisher eventos, RelogioSalao relogio) {
        this.repository = repository;
        this.pixService = pixService;
        this.mercadoPago = mercadoPago;
        this.notificacao = notificacao;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    /** Gera o Pix logo depois da reserva. Se não der, libera o horário. */
    @Transactional(noRollbackFor = RegraNegocioException.class)
    public SinalInfo iniciar(Long agendamentoId) {
        Agendamento a = carregar(agendamentoId);
        PixGerado pix;
        try {
            pix = pixService.gerar(a, a.getValorSinal(), a.getSinalExpiraEm());
        } catch (RuntimeException e) {
            log.error("Falha ao gerar o Pix do sinal do agendamento {}", agendamentoId, e);
            a.alterarStatus(StatusAgendamento.CANCELADO);
            eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
            throw new RegraNegocioException("Não foi possível gerar o Pix do sinal agora. Tente novamente em instantes.");
        }
        a.registrarPix(pix.pagamentoId(), pix.copiaCola());
        return new SinalInfo(a.getCodigoPublico().toString(), a.getValorSinal(),
                a.getPrecoCobrado().subtract(a.getValorSinal()), pix.copiaCola(), pix.qrCodeBase64(),
                relogio.paraLocal(a.getSinalExpiraEm()), pixService.automatico());
    }

    /** O site pergunta a cada poucos segundos: já pagou? */
    @Transactional(readOnly = true)
    public StatusSinal status(UUID codigo) {
        Agendamento a = repository.findByCodigoPublico(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado."));
        return new StatusSinal(a.getStatus().name(),
                a.getSinalExpiraEm() == null ? null : relogio.paraLocal(a.getSinalExpiraEm()));
    }

    /** Equipe confirmou no painel que o Pix caiu (modo manual). */
    @Transactional
    public AgendamentoResumo confirmarPelaEquipe(Long agendamentoId) {
        Agendamento a = carregar(agendamentoId);
        if (a.getStatus() != StatusAgendamento.AGUARDANDO_SINAL) {
            throw new RegraNegocioException("Este agendamento não está aguardando sinal.");
        }
        return confirmar(a);
    }

    /** Aviso do Mercado Pago: confere o pagamento lá antes de aceitar. */
    @Transactional
    public void processarAvisoMercadoPago(String pagamentoId) {
        if (!mercadoPago.configurado() || pagamentoId == null) {
            return;
        }
        MercadoPagoClient.Consulta c = mercadoPago.consultar(pagamentoId);
        if (!c.aprovado()) {
            return;
        }
        repository.findByPagamentoId(pagamentoId).ifPresent(a -> {
            if (a.getStatus() == StatusAgendamento.AGUARDANDO_SINAL) {
                confirmar(a);
            } else if (a.getSinalPagoEm() == null && a.getStatus() == StatusAgendamento.CANCELADO) {
                a.registrarSinalPagoAposPrazo(relogio.agoraOffset());
                eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
                log.warn("Sinal do agendamento {} pago depois do prazo: verificar devolução", a.getId());
            }
        });
    }

    /**
     * A cada minuto: confere no Mercado Pago os sinais em aberto. Funciona mesmo
     * sem os avisos (webhook), por exemplo no computador de testes.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void conferirPagamentosEmAberto() {
        if (!mercadoPago.configurado()) {
            return;
        }
        for (Agendamento a : repository.findSinaisEmAbertoComPagamento()) {
            try {
                if (mercadoPago.consultar(a.getPagamentoId()).aprovado()) {
                    confirmar(a);
                }
            } catch (RuntimeException e) {
                log.warn("Não foi possível conferir o pagamento {}: {}", a.getPagamentoId(), e.getMessage());
            }
        }
    }

    /** A cada minuto: prazo acabou sem pagamento → libera o horário e avisa a cliente. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 45_000)
    @Transactional
    public void liberarVencidos() {
        OffsetDateTime agora = relogio.agoraOffset();
        List<Agendamento> vencidos = repository.findSinaisVencidos(agora);
        for (Agendamento a : vencidos) {
            // última conferência: pode ter pago no último minuto
            if (a.getPagamentoId() != null && mercadoPago.configurado()) {
                try {
                    if (mercadoPago.consultar(a.getPagamentoId()).aprovado()) {
                        confirmar(a);
                        continue;
                    }
                } catch (RuntimeException e) {
                    log.warn("Não foi possível conferir o pagamento {}: {}", a.getPagamentoId(), e.getMessage());
                }
            }
            a.alterarStatus(StatusAgendamento.CANCELADO);
            AgendamentoResumo r = AgendamentoResumo.de(a, relogio);
            depoisDeGravar(() -> notificacao.sinalExpirado(r));
        }
        if (!vencidos.isEmpty()) {
            eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
        }
    }

    private AgendamentoResumo confirmar(Agendamento a) {
        a.registrarSinalPago(relogio.agoraOffset());
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
        AgendamentoResumo r = AgendamentoResumo.de(a, relogio);
        depoisDeGravar(() -> notificacao.sinalRecebido(r));
        return r;
    }

    private static void depoisDeGravar(Runnable acao) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    acao.run();
                }
            });
        } else {
            acao.run();
        }
    }

    private Agendamento carregar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado."));
    }
}
