package br.com.jofashionhair.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Produto;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.ProdutoRepository;

/** Vitrine de produtos. Toda alteração aparece no site na hora (evento "produtos"). */
@Service
@Transactional
public class ProdutoService {

    private final ProdutoRepository repository;
    private final ApplicationEventPublisher eventos;

    public ProdutoService(ProdutoRepository repository, ApplicationEventPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<ProdutoDto> vitrine() {
        return repository.findByAtivoTrueOrderByOrdemAscNomeAsc().stream().map(ProdutoDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoDto> listar() {
        return repository.findAllByOrderByAtivoDescOrdemAscNomeAsc().stream().map(ProdutoDto::de).toList();
    }

    public ProdutoDto criar(ProdutoDto dto) {
        Produto p = new Produto();
        aplicar(p, dto);
        repository.save(p);
        publicar();
        return ProdutoDto.de(p);
    }

    public ProdutoDto atualizar(Long id, ProdutoDto dto) {
        Produto p = buscar(id);
        aplicar(p, dto);
        publicar();
        return ProdutoDto.de(p);
    }

    public void remover(Long id) {
        repository.delete(buscar(id));
        publicar();
    }

    private void aplicar(Produto p, ProdutoDto dto) {
        p.atualizar(dto.nome().trim(), vazioParaNull(dto.marca()), vazioParaNull(dto.descricao()), dto.preco(),
                vazioParaNull(dto.fotoUrl()),
                dto.disponivel() == null || dto.disponivel(),
                dto.ordem() == null ? 0 : dto.ordem(),
                dto.ativo() == null || dto.ativo());
    }

    private Produto buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.PRODUTOS));
    }

    private static String vazioParaNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
