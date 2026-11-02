package br.com.jofashionhair.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.FotoLookbook;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.FotoLookbookRepository;

/** Lookbook do site. Mudanças aparecem na hora (evento "lookbook"). */
@Service
@Transactional
public class LookbookService {

    private final FotoLookbookRepository repository;
    private final ApplicationEventPublisher eventos;

    public LookbookService(FotoLookbookRepository repository, ApplicationEventPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<FotoLookbookDto> publicas() {
        return repository.findByAtivoTrueOrderByOrdemAscIdDesc().stream().map(FotoLookbookDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<FotoLookbookDto> listar() {
        return repository.findAllByOrderByAtivoDescOrdemAscIdDesc().stream().map(FotoLookbookDto::de).toList();
    }

    public FotoLookbookDto criar(FotoLookbookDto dto) {
        FotoLookbook f = new FotoLookbook();
        aplicar(f, dto);
        repository.save(f);
        publicar();
        return FotoLookbookDto.de(f);
    }

    public FotoLookbookDto atualizar(Long id, FotoLookbookDto dto) {
        FotoLookbook f = buscar(id);
        aplicar(f, dto);
        publicar();
        return FotoLookbookDto.de(f);
    }

    public void remover(Long id) {
        repository.delete(buscar(id));
        publicar();
    }

    private void aplicar(FotoLookbook f, FotoLookbookDto dto) {
        f.atualizar(dto.fotoUrl().trim(),
                dto.legenda() == null || dto.legenda().isBlank() ? null : dto.legenda().trim(),
                dto.ordem() == null ? 0 : dto.ordem(),
                dto.ativo() == null || dto.ativo());
    }

    private FotoLookbook buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Foto não encontrada."));
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.LOOKBOOK));
    }
}
