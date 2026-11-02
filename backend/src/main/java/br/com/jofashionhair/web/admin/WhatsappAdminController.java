package br.com.jofashionhair.web.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.domain.EstadoConversa;
import br.com.jofashionhair.whatsapp.AtendimentoWhatsappService;
import br.com.jofashionhair.whatsapp.ConversaResumo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * /api/admin/whatsapp
 * <pre>
 * POST /simular                          conversa com o robô SEM WhatsApp (testes)
 *      {"telefone":"47999990000","nome":"Maria","texto":"quero unha em gel sexta às 17h"}
 * GET  /conversas[?estado=ATENDIMENTO_HUMANO]
 * POST /conversas/{telefone}/reativar-bot
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/whatsapp")
public class WhatsappAdminController {

    private final AtendimentoWhatsappService atendimento;

    public WhatsappAdminController(AtendimentoWhatsappService atendimento) {
        this.atendimento = atendimento;
    }

    public record SimulacaoRequest(@NotBlank String telefone, String nome, @NotBlank String texto) {
    }

    /**
     * Passa pelo MESMO fluxo do WhatsApp real (inclusive cria agendamentos de
     * verdade), mas devolve a resposta aqui em vez de enviá-la.
     */
    @PostMapping("/simular")
    public Map<String, String> simular(@Valid @RequestBody SimulacaoRequest req) {
        String resposta = atendimento.processarTexto(req.telefone(), req.nome(), req.texto());
        return Map.of("resposta", resposta == null ? "(robô em silêncio: conversa em atendimento humano)" : resposta);
    }

    @GetMapping("/conversas")
    public List<ConversaResumo> conversas(@RequestParam(required = false) EstadoConversa estado) {
        return atendimento.listarConversas(estado);
    }

    @PostMapping("/conversas/{telefone}/reativar-bot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reativarBot(@PathVariable String telefone) {
        atendimento.reativarRobo(telefone);
    }
}
