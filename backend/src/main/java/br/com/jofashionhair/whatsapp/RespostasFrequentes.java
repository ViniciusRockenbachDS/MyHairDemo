package br.com.jofashionhair.whatsapp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.PerguntaFrequente;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.repository.PerguntaFrequenteRepository;
import br.com.jofashionhair.service.CatalogoAlteradoEvent;
import br.com.jofashionhair.service.PerguntaFrequenteDto;

/**
 * Perguntas frequentes do robô (sem IA, sem custo por mensagem).
 *
 * <p>Cada pergunta tem palavras-chave. A mensagem da cliente é comparada sem
 * acentos e sem diferenciar maiúsculas; plural também conta ("cartões" casa
 * com "cartão"). Expressões de mais de uma palavra ("forma de pagamento")
 * valem mais. Vence a pergunta com mais pontos; empate fica com a de menor ordem.</p>
 */
@Service
@Transactional
public class RespostasFrequentes {

    private final PerguntaFrequenteRepository repository;
    private final InterpretadorMensagem interpretador;
    private final ApplicationEventPublisher eventos;

    public RespostasFrequentes(PerguntaFrequenteRepository repository, InterpretadorMensagem interpretador,
                               ApplicationEventPublisher eventos) {
        this.repository = repository;
        this.interpretador = interpretador;
        this.eventos = eventos;
    }

    /** @param texto mensagem da cliente (original ou já normalizada) */
    @Transactional(readOnly = true)
    public Optional<PerguntaFrequente> encontrar(String texto) {
        String n = apenasPalavras(interpretador.normalizar(texto));
        if (n.isEmpty()) {
            return Optional.empty();
        }
        String frase = " " + n + " ";
        Set<String> palavras = new HashSet<>();
        Arrays.stream(n.split(" ")).map(RespostasFrequentes::singular).forEach(palavras::add);

        PerguntaFrequente melhor = null;
        int melhorPontos = 0;
        for (PerguntaFrequente p : repository.findByAtivoTrueOrderByOrdemAscIdAsc()) {
            int pontos = 0;
            for (String bruta : p.getPalavrasChave().split("[,;\\n]")) {
                String chave = apenasPalavras(interpretador.normalizar(bruta));
                if (chave.isEmpty()) {
                    continue;
                }
                if (chave.contains(" ")) {
                    if (frase.contains(" " + chave + " ")) {
                        pontos += chave.split(" ").length * 2;
                    }
                } else if (palavras.contains(singular(chave))) {
                    pontos += 1;
                }
            }
            if (pontos > melhorPontos) {   // empate: fica a primeira (menor ordem)
                melhor = p;
                melhorPontos = pontos;
            }
        }
        return Optional.ofNullable(melhor);
    }

    // ================================================================== painel

    @Transactional(readOnly = true)
    public List<PerguntaFrequenteDto> listar() {
        return repository.findAllByOrderByOrdemAscIdAsc().stream().map(PerguntaFrequenteDto::de).toList();
    }

    public PerguntaFrequenteDto criar(PerguntaFrequenteDto dto) {
        PerguntaFrequente p = new PerguntaFrequente();
        aplicar(p, dto);
        repository.save(p);
        publicar();
        return PerguntaFrequenteDto.de(p);
    }

    public PerguntaFrequenteDto atualizar(Long id, PerguntaFrequenteDto dto) {
        PerguntaFrequente p = buscar(id);
        aplicar(p, dto);
        publicar();
        return PerguntaFrequenteDto.de(p);
    }

    public void remover(Long id) {
        repository.delete(buscar(id));
        publicar();
    }

    private void aplicar(PerguntaFrequente p, PerguntaFrequenteDto dto) {
        p.atualizar(dto.pergunta().trim(), dto.palavrasChave().trim(), dto.resposta().trim(),
                dto.ordem() == null ? 0 : dto.ordem(), dto.ativo() == null || dto.ativo());
    }

    private PerguntaFrequente buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta não encontrada."));
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.SALAO));
    }

    private static String apenasPalavras(String s) {
        return s.replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** "cartoes" → "cartao", "unhas" → "unha", "atrasos" → "atraso". */
    private static String singular(String t) {
        if (t.length() > 4 && t.endsWith("oes")) {
            return t.substring(0, t.length() - 3) + "ao";
        }
        if (t.length() > 4 && t.endsWith("aes")) {
            return t.substring(0, t.length() - 3) + "ao";
        }
        if (t.length() > 3 && t.endsWith("s")) {
            return t.substring(0, t.length() - 1);
        }
        return t;
    }
}
