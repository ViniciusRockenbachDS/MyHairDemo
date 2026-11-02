package br.com.jofashionhair.web.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.domain.OrigemAgendamento;
import br.com.jofashionhair.lembrete.LembreteService;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.AgendamentoService;
import br.com.jofashionhair.service.SolicitacaoAgendamento;
import br.com.jofashionhair.service.admin.AdminDtos.AgendamentoManualRequest;
import br.com.jofashionhair.service.admin.AdminDtos.StatusRequest;
import br.com.jofashionhair.service.admin.AgendaAdminService;
import jakarta.validation.Valid;

/**
 * /api/admin/agendamentos
 * <pre>
 * GET   /?de=2026-10-16&ate=2026-10-16[&profissionalId=2][&status=CONFIRMADO]
 * GET   /{id}
 * POST  /                agendamento manual (balcão/telefone) — ignora a antecedência mínima
 * PATCH /{id}/status     {"status":"FALTOU"}  (CONFIRMADO, CONCLUIDO, CANCELADO, FALTOU)
 * GET   /sem-baixa       atendimentos que já passaram e ainda não foram marcados (concluído/faltou)
 * POST  /{id}/lembrete   envia agora o lembrete com pedido de confirmação
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/agendamentos")
public class AgendaAdminController {

    private final AgendaAdminService agendaService;
    private final AgendamentoService agendamentoService;
    private final LembreteService lembreteService;

    public AgendaAdminController(AgendaAdminService agendaService,
                                 AgendamentoService agendamentoService,
                                 LembreteService lembreteService) {
        this.agendaService = agendaService;
        this.agendamentoService = agendamentoService;
        this.lembreteService = lembreteService;
    }

    @GetMapping("/sem-baixa")
    public List<AgendamentoResumo> semBaixa() {
        return agendaService.semBaixa();
    }

    @PostMapping("/{id}/lembrete")
    public Map<String, Boolean> enviarLembrete(@PathVariable Long id) {
        return Map.of("enviado", lembreteService.enviarAgora(id));
    }

    @GetMapping
    public List<AgendamentoResumo> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long profissionalId,
            @RequestParam(required = false) StatusAgendamento status) {
        return agendaService.listar(de, ate, profissionalId, status);
    }

    @GetMapping("/{id}")
    public AgendamentoResumo buscar(@PathVariable Long id) {
        return agendamentoService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResumo criarManual(@Valid @RequestBody AgendamentoManualRequest req) {
        return agendamentoService.reservar(new SolicitacaoAgendamento(
                req.servicoId(),
                req.profissionalId(),
                req.data().atTime(req.hora()),
                req.nome(),
                req.telefone(),
                OrigemAgendamento.ADMIN,
                req.observacoes()));
    }

    @PatchMapping("/{id}/status")
    public AgendamentoResumo alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return agendamentoService.alterarStatus(id, req.status());
    }
}
