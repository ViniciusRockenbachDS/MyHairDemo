package br.com.jofashionhair.pagamento;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import br.com.jofashionhair.domain.Agendamento;
import br.com.jofashionhair.service.SalaoInformacoesDto;
import br.com.jofashionhair.service.SalaoService;

/**
 * Gera o Pix do sinal:
 * <ul>
 *   <li><b>Automático</b> (Mercado Pago configurado): a confirmação chega sozinha.</li>
 *   <li><b>Manual</b> (chave Pix do salão): a equipe confirma no painel.</li>
 * </ul>
 */
@Service
public class PixService {

    private final MercadoPagoClient mercadoPago;
    private final SalaoService salaoService;

    public PixService(MercadoPagoClient mercadoPago, SalaoService salaoService) {
        this.mercadoPago = mercadoPago;
        this.salaoService = salaoService;
    }

    public boolean automatico() {
        return mercadoPago.configurado();
    }

    /** Dá para cobrar sinal? (Mercado Pago ou chave Pix do salão.) */
    public boolean configurado() {
        if (automatico()) return true;
        SalaoInformacoesDto s = salaoService.informacoes();
        return s.chavePix() != null && !s.chavePix().isBlank();
    }

    public PixGerado gerar(Agendamento a, BigDecimal valor, OffsetDateTime expiraEm) {
        String referencia = a.getCodigoPublico().toString();
        if (automatico()) {
            return mercadoPago.criarPix(valor, "Sinal - " + a.getServico().getNome(), referencia, expiraEm);
        }
        SalaoInformacoesDto s = salaoService.informacoes();
        String nome = s.pixNomeRecebedor() != null ? s.pixNomeRecebedor() : s.nome();
        String cidade = s.pixCidade() != null ? s.pixCidade() : (s.cidade() != null ? s.cidade() : "BRASIL");
        String copiaCola = BrCodePix.gerar(s.chavePix(), nome, cidade, valor, "SINAL" + a.getId());
        return new PixGerado(null, copiaCola, QrCodePng.base64(copiaCola));
    }
}
