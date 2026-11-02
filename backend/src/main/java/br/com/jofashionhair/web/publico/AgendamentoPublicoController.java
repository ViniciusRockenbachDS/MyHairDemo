package br.com.jofashionhair.web.publico;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.jofashionhair.domain.OrigemAgendamento;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.AgendamentoService;
import br.com.jofashionhair.service.AvaliacaoService;
import br.com.jofashionhair.service.AvaliacoesPublicas;
import br.com.jofashionhair.service.CatalogoEventos;
import br.com.jofashionhair.service.CatalogoPublicoService;
import br.com.jofashionhair.service.DisponibilidadeService;
import br.com.jofashionhair.service.HorarioFuncionamentoDto;
import br.com.jofashionhair.service.HorariosProfissional;
import br.com.jofashionhair.service.MembroEquipe;
import br.com.jofashionhair.promocao.PromocaoDtos;
import br.com.jofashionhair.promocao.PromocaoService;
import br.com.jofashionhair.service.FotoLookbookDto;
import br.com.jofashionhair.service.LookbookService;
import br.com.jofashionhair.service.ProdutoDto;
import br.com.jofashionhair.service.ProdutoService;
import br.com.jofashionhair.service.SalaoService;
import br.com.jofashionhair.service.SolicitacaoAgendamento;
import br.com.jofashionhair.whatsapp.NotificacaoWhatsapp;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * API pública consumida pelo site de agendamento (React).
 */
@RestController
@RequestMapping("/api/public")
public class AgendamentoPublicoController {

    private final CatalogoPublicoService catalogo;
    private final DisponibilidadeService disponibilidade;
    private final AgendamentoService agendamentoService;
    private final CatalogoEventos catalogoEventos;
    private final SalaoService salaoService;
    private final NotificacaoWhatsapp notificacao;
    private final AvaliacaoService avaliacaoService;
    private final ProdutoService produtoService;
    private final PromocaoService promocaoService;
    private final LookbookService lookbookService;
    private final br.com.jofashionhair.pagamento.SinalService sinalService;

    public AgendamentoPublicoController(CatalogoPublicoService catalogo,
                                        DisponibilidadeService disponibilidade,
                                        AgendamentoService agendamentoService,
                                        CatalogoEventos catalogoEventos,
                                        SalaoService salaoService,
                                        NotificacaoWhatsapp notificacao,
                                        AvaliacaoService avaliacaoService,
                                        ProdutoService produtoService,
                                        PromocaoService promocaoService,
                                        LookbookService lookbookService,
                                        br.com.jofashionhair.pagamento.SinalService sinalService) {
        this.catalogo = catalogo;
        this.disponibilidade = disponibilidade;
        this.agendamentoService = agendamentoService;
        this.catalogoEventos = catalogoEventos;
        this.salaoService = salaoService;
        this.notificacao = notificacao;
        this.avaliacaoService = avaliacaoService;
        this.produtoService = produtoService;
        this.promocaoService = promocaoService;
        this.lookbookService = lookbookService;
        this.sinalService = sinalService;
    }

    /** GET /api/public/lookbook → fotos de trabalhos feitos no salão. */
    @GetMapping("/lookbook")
    public List<FotoLookbookDto> lookbook() {
        return lookbookService.publicas();
    }

    /** GET /api/public/promocoes → promoções ativas (site mostra e aplica no preço). */
    @GetMapping("/promocoes")
    public List<PromocaoDtos.PromocaoDto> promocoes() {
        return promocaoService.vigentes();
    }

    /** GET /api/public/produtos → vitrine (só os visíveis). */
    @GetMapping("/produtos")
    public List<ProdutoDto> produtos() {
        return produtoService.vitrine();
    }

    /** GET /api/public/avaliacoes → resumo (nota, total, link) e avaliações visíveis. */
    @GetMapping("/avaliacoes")
    public AvaliacoesPublicas avaliacoes() {
        return avaliacaoService.publicas();
    }

    /** GET /api/public/profissionais → equipe com as funções/serviços de cada uma. */
    @GetMapping("/profissionais")
    public List<MembroEquipe> equipe() {
        return catalogo.equipe();
    }

    /** GET /api/public/salao → nome, endereço, contatos, Instagram e horário de funcionamento. */
    @GetMapping("/salao")
    public SalaoService.SalaoPublico salao() {
        return salaoService.dadosPublicos();
    }

    /** GET /api/public/salao/horarios → só o horário de funcionamento. */
    @GetMapping("/salao/horarios")
    public List<HorarioFuncionamentoDto> horariosSalao() {
        return salaoService.listarHorarios();
    }

    /**
     * GET /api/public/eventos (Server-Sent Events)
     * O site escuta o evento "catalogo-atualizado" e recarrega serviços/horários.
     */
    @GetMapping(path = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");   // Nginx: não segurar os eventos em buffer
        return catalogoEventos.inscrever();
    }

    /** GET /api/public/servicos → serviços ativos com preço e duração atuais. */
    @GetMapping("/servicos")
    public List<ServicoPublicoResponse> servicos() {
        return catalogo.servicosAtivos().stream().map(ServicoPublicoResponse::de).toList();
    }

    /** GET /api/public/servicos/{id}/profissionais → quem realiza o serviço. */
    @GetMapping("/servicos/{id}/profissionais")
    public List<ProfissionalPublicoResponse> profissionais(@PathVariable Long id) {
        return catalogo.profissionaisDoServico(id).stream().map(ProfissionalPublicoResponse::de).toList();
    }

    /** GET /api/public/disponibilidade?servicoId=4&data=2026-10-16[&profissionalId=2] */
    @GetMapping("/disponibilidade")
    public List<HorariosProfissional> disponibilidade(
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Long profissionalId) {
        return disponibilidade.horariosLivresDoDia(servicoId, profissionalId, data);
    }

    /**
     * POST /api/public/agendamentos
     * 201 = reservado | 409 = indisponível (a resposta traz "sugestoes")
     */
    /** Resposta da reserva: o agendamento + o Pix do sinal (quando o serviço cobra sinal). */
    public record ReservaSite(
            @com.fasterxml.jackson.annotation.JsonUnwrapped AgendamentoResumo agendamento,
            br.com.jofashionhair.pagamento.SinalService.SinalInfo sinal) {
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaSite agendar(@Valid @RequestBody NovoAgendamentoRequest req) {
        AgendamentoResumo reserva = agendamentoService.reservar(new SolicitacaoAgendamento(
                req.servicoId(),
                req.profissionalId(),
                req.data().atTime(req.hora()),
                req.nome(),
                req.telefone(),
                OrigemAgendamento.SITE,
                req.observacoes()));
        if (Boolean.TRUE.equals(req.aceitaPromocoes())) {
            promocaoService.registrarPreferencia(req.telefone(), true);
        }
        if (reserva.status() == br.com.jofashionhair.domain.StatusAgendamento.AGUARDANDO_SINAL) {
            // o aviso pelo WhatsApp sai quando o sinal for pago
            return new ReservaSite(reserva, sinalService.iniciar(reserva.id()));
        }
        notificacao.agendamentoPeloSite(reserva);   // em segundo plano: não atrasa a resposta
        return new ReservaSite(reserva, null);
    }
}
