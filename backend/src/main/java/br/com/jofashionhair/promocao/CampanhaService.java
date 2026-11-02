package br.com.jofashionhair.promocao;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.jofashionhair.domain.Cliente;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.promocao.PromocaoDtos.CampanhaResponse;
import br.com.jofashionhair.promocao.PromocaoDtos.EnvioRequest;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.service.RelogioSalao;

/**
 * Envio de promoções por WhatsApp para clientes com histórico
 * (pelo menos um atendimento concluído).
 *
 * <ul>
 *   <li>Nunca envia para quem respondeu SAIR.</li>
 *   <li>Recomendado: só para quem autorizou receber promoções.</li>
 *   <li>Um envio por vez.</li>
 * </ul>
 */
@Service
public class CampanhaService {

    private final PromocaoRepository promocaoRepository;
    private final CampanhaWhatsappRepository campanhaRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final EnvioCampanhaWorker worker;
    private final RelogioSalao relogio;

    public CampanhaService(PromocaoRepository promocaoRepository, CampanhaWhatsappRepository campanhaRepository,
                           AgendamentoRepository agendamentoRepository, EnvioCampanhaWorker worker, RelogioSalao relogio) {
        this.promocaoRepository = promocaoRepository;
        this.campanhaRepository = campanhaRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.worker = worker;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public int contarPublico(Long promocaoId, boolean soAutorizadas, boolean soQuemFezServicos) {
        return destinatarios(buscar(promocaoId), soAutorizadas, soQuemFezServicos).size();
    }

    @Transactional
    public CampanhaResponse enviar(Long promocaoId, EnvioRequest req) {
        if (campanhaRepository.existsByStatus("ENVIANDO")) {
            throw new RegraNegocioException("Já existe um envio em andamento. Aguarde ele terminar.");
        }
        Promocao p = buscar(promocaoId);
        boolean soAutorizadas = req.soAutorizadas() == null || req.soAutorizadas();
        boolean soQuemFez = Boolean.TRUE.equals(req.soQuemFezServicos());
        List<Destinatario> lista = destinatarios(p, soAutorizadas, soQuemFez).stream()
                .map(c -> new Destinatario(c.getNome(), c.getTelefone()))
                .toList();
        if (lista.isEmpty()) {
            throw new RegraNegocioException("Nenhuma cliente se encaixa nesse público.");
        }
        String publico = (soAutorizadas ? "Autorizaram promoções" : "Todas com histórico")
                + (soQuemFez ? ", que já fizeram os serviços" : "");
        CampanhaWhatsapp c = campanhaRepository.save(new CampanhaWhatsapp(p.getId(), p.getTitulo(),
                req.mensagem().trim(), publico, lista.size(), relogio.agoraOffset()));

        // começa a enviar só depois de gravar (o envio roda em segundo plano)
        Long id = c.getId();
        String mensagem = req.mensagem().trim();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                worker.executar(id, lista, mensagem);
            }
        });
        return resposta(c);
    }

    @Transactional(readOnly = true)
    public List<CampanhaResponse> historico() {
        return campanhaRepository.findTop30ByOrderByIdDesc().stream().map(this::resposta).toList();
    }

    private List<Cliente> destinatarios(Promocao p, boolean soAutorizadas, boolean soQuemFezServicos) {
        List<Cliente> base = soQuemFezServicos && !p.isTodosServicos()
                ? agendamentoRepository.findClientesComHistoricoNosServicos(
                        p.getServicos().stream().map(Servico::getId).toList())
                : agendamentoRepository.findClientesComHistorico();
        return base.stream()
                .filter(c -> c.getTelefone() != null)
                .filter(c -> soAutorizadas ? Boolean.TRUE.equals(c.getAceitaPromocoes())
                        : !Boolean.FALSE.equals(c.getAceitaPromocoes()))   // nunca quem pediu para sair
                .toList();
    }

    private CampanhaResponse resposta(CampanhaWhatsapp c) {
        return new CampanhaResponse(c.getId(), c.getTitulo(), c.getPublico(), c.getTotal(), c.getEnviados(),
                c.getFalhas(), c.getStatus(), relogio.paraLocal(c.getCriadoEm()));
    }

    private Promocao buscar(Long id) {
        return promocaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Promoção não encontrada."));
    }
}
