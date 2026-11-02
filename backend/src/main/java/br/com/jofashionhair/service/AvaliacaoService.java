package br.com.jofashionhair.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Avaliacao;
import br.com.jofashionhair.domain.ConfiguracaoSalao;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.AvaliacaoRepository;
import br.com.jofashionhair.repository.ConfiguracaoSalaoRepository;

/**
 * Avaliações cadastradas pelo dono. Toda alteração dispara o evento
 * "avaliacoes" e o site atualiza a seção na hora.
 */
@Service
@Transactional
public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final ConfiguracaoSalaoRepository configuracaoRepository;
    private final ApplicationEventPublisher eventos;

    public AvaliacaoService(AvaliacaoRepository repository,
                            ConfiguracaoSalaoRepository configuracaoRepository,
                            ApplicationEventPublisher eventos) {
        this.repository = repository;
        this.configuracaoRepository = configuracaoRepository;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public AvaliacoesPublicas publicas() {
        ConfiguracaoSalao c = configuracao();
        List<AvaliacaoDto> lista = repository.findByAtivoTrueOrderByOrdemAscDataDescIdDesc().stream()
                .map(AvaliacaoDto::de)
                .toList();
        return new AvaliacoesPublicas(c.getAvaliacoesNota(), c.getAvaliacoesTotal(), c.getAvaliacoesLink(), lista);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoDto> listar() {
        return repository.findAllByOrderByOrdemAscDataDescIdDesc().stream().map(AvaliacaoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public ResumoAvaliacoesDto resumo() {
        ConfiguracaoSalao c = configuracao();
        return new ResumoAvaliacoesDto(c.getAvaliacoesNota(), c.getAvaliacoesTotal(), c.getAvaliacoesLink());
    }

    public ResumoAvaliacoesDto salvarResumo(ResumoAvaliacoesDto dto) {
        configuracao().atualizarResumoAvaliacoes(dto.nota(), dto.total(), vazioParaNull(dto.link()));
        publicar();
        return dto;
    }

    public AvaliacaoDto criar(AvaliacaoDto dto) {
        Avaliacao a = new Avaliacao();
        aplicar(a, dto);
        repository.save(a);
        publicar();
        return AvaliacaoDto.de(a);
    }

    public AvaliacaoDto atualizar(Long id, AvaliacaoDto dto) {
        Avaliacao a = buscar(id);
        aplicar(a, dto);
        publicar();
        return AvaliacaoDto.de(a);
    }

    public void remover(Long id) {
        repository.delete(buscar(id));
        publicar();
    }

    // ------------------------------------------------------------------ privados

    private void aplicar(Avaliacao a, AvaliacaoDto dto) {
        a.atualizar(dto.autor().trim(), dto.texto().trim(), dto.nota(), dto.data(),
                vazioParaNull(dto.fotoUrl()), vazioParaNull(dto.link()),
                dto.ordem() == null ? 0 : dto.ordem(),
                dto.ativo() == null || dto.ativo());
    }

    private Avaliacao buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada."));
    }

    private ConfiguracaoSalao configuracao() {
        return configuracaoRepository.findById(ConfiguracaoSalao.ID_UNICO)
                .orElseThrow(() -> new IllegalStateException("Configuração do salão ausente."));
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.AVALIACOES));
    }

    private static String vazioParaNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
