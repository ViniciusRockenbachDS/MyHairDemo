package br.com.jofashionhair.pagamento;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.jofashionhair.service.AgendamentoResumo;

/**
 * <pre>
 * GET  /api/public/sinais/{codigo}                    site: o sinal já foi pago?
 * POST /api/webhooks/mercadopago                       aviso de pagamento do Mercado Pago
 * POST /api/admin/agendamentos/{id}/sinal-recebido     equipe confirma o Pix (modo manual)
 * GET  /api/admin/pagamentos/situacao                  painel: modo automático ligado?
 * </pre>
 */
@RestController
public class PagamentoController {

    private final SinalService sinalService;
    private final MercadoPagoClient mercadoPago;
    private final PixService pixService;

    public PagamentoController(SinalService sinalService, MercadoPagoClient mercadoPago, PixService pixService) {
        this.sinalService = sinalService;
        this.mercadoPago = mercadoPago;
        this.pixService = pixService;
    }

    @GetMapping("/api/public/sinais/{codigo}")
    public SinalService.StatusSinal status(@PathVariable UUID codigo) {
        return sinalService.status(codigo);
    }

    /**
     * O Mercado Pago avisa com {"type":"payment","data":{"id":"123"}} (ou pela URL).
     * Sempre responde 200 rápido; o pagamento é conferido direto no Mercado Pago.
     */
    @PostMapping("/api/webhooks/mercadopago")
    public ResponseEntity<Void> avisoMercadoPago(
            @RequestBody(required = false) Map<String, Object> corpo,
            @RequestParam(name = "data.id", required = false) String idPelaUrl,
            @RequestParam(name = "type", required = false) String tipoPelaUrl,
            @RequestHeader(name = "x-signature", required = false) String assinatura,
            @RequestHeader(name = "x-request-id", required = false) String requestId) {
        String tipo = tipoPelaUrl;
        String id = idPelaUrl;
        if (corpo != null) {
            if (tipo == null && corpo.get("type") != null) tipo = String.valueOf(corpo.get("type"));
            if (id == null && corpo.get("data") instanceof Map<?, ?> dados && dados.get("id") != null) {
                id = String.valueOf(dados.get("id"));
            }
        }
        if (!"payment".equals(tipo) || id == null) {
            return ResponseEntity.ok().build();
        }
        if (!mercadoPago.assinaturaValida(assinatura, requestId, id)) {
            return ResponseEntity.status(401).build();
        }
        sinalService.processarAvisoMercadoPago(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/admin/agendamentos/{id}/sinal-recebido")
    public AgendamentoResumo sinalRecebido(@PathVariable Long id) {
        return sinalService.confirmarPelaEquipe(id);
    }

    @GetMapping("/api/admin/pagamentos/situacao")
    public Map<String, Boolean> situacao() {
        return Map.of("automatico", pixService.automatico(), "configurado", pixService.configurado());
    }
}
