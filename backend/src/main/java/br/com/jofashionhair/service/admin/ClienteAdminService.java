package br.com.jofashionhair.service.admin;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.AgendamentoRepository;
import br.com.jofashionhair.repository.ClienteRepository;
import br.com.jofashionhair.repository.HistoricoCliente;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.RelogioSalao;

/**
 * Clientes e histórico de comparecimento (controle de no-show).
 */
@Service
@Transactional(readOnly = true)
public class ClienteAdminService {

    private static final int LIMITE_RESULTADOS = 50;

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final RelogioSalao relogio;

    public ClienteAdminService(ClienteRepository clienteRepository,
                               AgendamentoRepository agendamentoRepository,
                               RelogioSalao relogio) {
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.relogio = relogio;
    }

    /** Busca por nome ou telefone; sem termo, lista os primeiros 50 em ordem alfabética. */
    public List<HistoricoCliente> buscar(String termo) {
        String nome = termo == null ? "" : termo.trim();
        String digitos = nome.replaceAll("\\D", "");
        // "x" nunca casa com um telefone (só dígitos): evita que uma busca por
        // nome traga todos os clientes pelo filtro de telefone
        return clienteRepository.buscarComHistorico(nome, digitos.isEmpty() ? "x" : digitos,
                PageRequest.of(0, LIMITE_RESULTADOS));
    }

    public List<AgendamentoResumo> agendamentosDoCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado.");
        }
        return agendamentoRepository.findDoClienteComDetalhes(clienteId).stream()
                .map(a -> AgendamentoResumo.de(a, relogio))
                .toList();
    }
}
