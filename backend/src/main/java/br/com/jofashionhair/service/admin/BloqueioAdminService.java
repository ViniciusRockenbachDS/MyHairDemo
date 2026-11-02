package br.com.jofashionhair.service.admin;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Agendamento;
import br.com.jofashionhair.domain.BloqueioAgenda;
import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.repository.BloqueioAgendaRepository;
import br.com.jofashionhair.repository.ProfissionalRepository;
import br.com.jofashionhair.service.CatalogoAlteradoEvent;
import br.com.jofashionhair.service.RelogioSalao;
import br.com.jofashionhair.service.admin.AdminDtos.BloqueioRequest;
import br.com.jofashionhair.service.admin.AdminDtos.BloqueioResponse;

/**
 * Bloqueios manuais de agenda (folga, curso, consulta...).
 */
@Service
@Transactional
public class BloqueioAdminService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final BloqueioAgendaRepository bloqueioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final RelogioSalao relogio;
    private final ApplicationEventPublisher eventos;

    public BloqueioAdminService(BloqueioAgendaRepository bloqueioRepository,
                                ProfissionalRepository profissionalRepository,
                                AgendamentoRepository agendamentoRepository,
                                RelogioSalao relogio,
                                ApplicationEventPublisher eventos) {
        this.bloqueioRepository = bloqueioRepository;
        this.profissionalRepository = profissionalRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.relogio = relogio;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<BloqueioResponse> listar(LocalDate de, LocalDate ate, Long profissionalId) {
        Periodo.validar(de, ate);
        OffsetDateTime inicio = relogio.paraOffset(de.atStartOfDay());
        OffsetDateTime fim = relogio.paraOffset(ate.plusDays(1).atStartOfDay());
        return bloqueioRepository.findPeriodoComProfissional(inicio, fim).stream()
                .filter(b -> profissionalId == null || Objects.equals(b.getProfissional().getId(), profissionalId))
                .map(this::resposta)
                .toList();
    }

    /**
     * Cria o bloqueio. Se já houver clientes agendados no período, recusa e
     * lista quem são: o salão decide se cancela/remarca antes de bloquear.
     * A linha da profissional é travada, então nenhuma reserva "entra" no
     * meio da verificação.
     */
    public BloqueioResponse criar(BloqueioRequest req) {
        if (!req.fim().isAfter(req.inicio())) {
            throw new RegraNegocioException("O fim do bloqueio deve ser depois do início.");
        }
        Profissional profissional = profissionalRepository.findByIdParaReserva(req.profissionalId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrada."));

        OffsetDateTime inicio = relogio.paraOffset(req.inicio());
        OffsetDateTime fim = relogio.paraOffset(req.fim());

        List<Agendamento> conflitos = agendamentoRepository.findOcupandoPeriodo(
                profissional.getId(), StatusAgendamento.OCUPAM_AGENDA, inicio, fim);
        if (!conflitos.isEmpty()) {
            String lista = conflitos.stream()
                    .map(a -> FMT.format(relogio.paraLocal(a.getDataInicio())) + " - "
                            + a.getCliente().getNome() + " (" + a.getServico().getNome() + ")")
                    .collect(Collectors.joining("; "));
            throw new RegraNegocioException("Existem " + conflitos.size()
                    + " agendamento(s) neste período: " + lista + ". Cancele ou remarque antes de bloquear.");
        }

        BloqueioAgenda bloqueio = bloqueioRepository.save(
                new BloqueioAgenda(profissional, inicio, fim, req.motivo()));
        publicar();
        return resposta(bloqueio);
    }

    public void remover(Long id) {
        if (!bloqueioRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Bloqueio não encontrado.");
        }
        bloqueioRepository.deleteById(id);
        publicar();
    }

    private BloqueioResponse resposta(BloqueioAgenda b) {
        return new BloqueioResponse(b.getId(), b.getProfissional().getId(), b.getProfissional().getNome(),
                relogio.paraLocal(b.getDataInicio()), relogio.paraLocal(b.getDataFim()), b.getMotivo());
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AGENDA));
    }
}
