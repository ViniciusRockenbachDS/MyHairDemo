package br.com.jofashionhair.service.admin;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.RelogioSalao;

/**
 * Leitura da agenda para o painel (visão de calendário/lista).
 * Criação e troca de status ficam no AgendamentoService (regras únicas
 * para site, WhatsApp e /admin).
 */
@Service
@Transactional(readOnly = true)
public class AgendaAdminService {

    private final AgendamentoRepository agendamentoRepository;
    private final RelogioSalao relogio;

    public AgendaAdminService(AgendamentoRepository agendamentoRepository, RelogioSalao relogio) {
        this.agendamentoRepository = agendamentoRepository;
        this.relogio = relogio;
    }

    /**
     * Agendamentos que começam entre {@code de} e {@code ate} (inclusive),
     * opcionalmente filtrados por profissional e/ou status.
     */
    public List<AgendamentoResumo> listar(LocalDate de, LocalDate ate, Long profissionalId,
                                          StatusAgendamento status) {
        Periodo.validar(de, ate);
        OffsetDateTime inicio = relogio.paraOffset(de.atStartOfDay());
        OffsetDateTime fim = relogio.paraOffset(ate.plusDays(1).atStartOfDay());

        return agendamentoRepository.findPeriodoComDetalhes(inicio, fim).stream()
                .filter(a -> profissionalId == null || Objects.equals(a.getProfissional().getId(), profissionalId))
                .filter(a -> status == null || a.getStatus() == status)
                .map(a -> AgendamentoResumo.de(a, relogio))
                .toList();
    }

    /**
     * Controle de no-show: atendimentos dos últimos 30 dias que já terminaram
     * mas seguem PENDENTE/CONFIRMADO. O salão marca CONCLUIDO ou FALTOU.
     */
    public List<AgendamentoResumo> semBaixa() {
        OffsetDateTime agora = relogio.agoraOffset();
        return agendamentoRepository.findSemBaixa(
                        List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO),
                        agora, agora.minusDays(30))
                .stream()
                .map(a -> AgendamentoResumo.de(a, relogio))
                .toList();
    }
}
