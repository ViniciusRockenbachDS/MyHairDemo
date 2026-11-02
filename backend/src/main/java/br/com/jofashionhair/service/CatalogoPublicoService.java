package br.com.jofashionhair.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.repository.ProfissionalRepository;
import br.com.jofashionhair.repository.ServicoRepository;

/**
 * Leituras do catálogo para o site público (e futuramente o robô).
 * Sem cache de propósito: preço/duração alterados no /admin aparecem na hora.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoPublicoService {

    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;

    public CatalogoPublicoService(ServicoRepository servicoRepository,
                                  ProfissionalRepository profissionalRepository) {
        this.servicoRepository = servicoRepository;
        this.profissionalRepository = profissionalRepository;
    }

    public List<Servico> servicosAtivos() {
        return servicoRepository.findByAtivoTrueOrderByOrdemExibicaoAscNomeAsc();
    }

    public List<Profissional> profissionaisDoServico(Long servicoId) {
        return profissionalRepository.findAtivasQueRealizam(servicoId);
    }

    /** Equipe do site: profissionais ativas com os serviços ativos que realizam. */
    public List<MembroEquipe> equipe() {
        return profissionalRepository.findAllComServicos().stream()
                .filter(Profissional::isAtivo)
                .map(p -> new MembroEquipe(p.getId(), p.getNome(), p.getEspecialidade(), p.getBio(), p.getFotoUrl(),
                        p.getServicos().stream()
                                .filter(Servico::isAtivo)
                                .sorted(Comparator.comparing(Servico::getOrdemExibicao).thenComparing(Servico::getNome))
                                .map(Servico::getNome)
                                .toList()))
                .toList();
    }
}
