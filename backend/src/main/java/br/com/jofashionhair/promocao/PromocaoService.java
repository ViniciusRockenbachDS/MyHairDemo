package br.com.jofashionhair.promocao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.domain.Telefone;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.promocao.PromocaoDtos.PromocaoDto;
import br.com.jofashionhair.repository.ClienteRepository;
import br.com.jofashionhair.repository.ServicoRepository;
import br.com.jofashionhair.service.CatalogoAlteradoEvent;
import br.com.jofashionhair.service.RelogioSalao;

/**
 * Promoções: X% de desconto em dias da semana, período e serviços escolhidos.
 * O desconto entra sozinho no preço do agendamento (site, WhatsApp e painel).
 * Se mais de uma valer, fica a de MAIOR desconto (não acumulam).
 */
@Service
@Transactional
public class PromocaoService {

    private static final String[] DIAS = {"", "segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo"};
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final PromocaoRepository repository;
    private final ServicoRepository servicoRepository;
    private final ClienteRepository clienteRepository;
    private final ApplicationEventPublisher eventos;
    private final RelogioSalao relogio;

    public PromocaoService(PromocaoRepository repository, ServicoRepository servicoRepository,
                           ClienteRepository clienteRepository, ApplicationEventPublisher eventos, RelogioSalao relogio) {
        this.repository = repository;
        this.servicoRepository = servicoRepository;
        this.clienteRepository = clienteRepository;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    // ================================================================== painel

    @Transactional(readOnly = true)
    public List<PromocaoDto> listar() {
        return repository.findAllComServicos().stream().map(PromocaoDto::de).toList();
    }

    public PromocaoDto criar(PromocaoDto dto) {
        Promocao p = new Promocao();
        aplicar(p, dto);
        repository.save(p);
        publicar();
        return PromocaoDto.de(p);
    }

    public PromocaoDto atualizar(Long id, PromocaoDto dto) {
        Promocao p = buscar(id);
        aplicar(p, dto);
        publicar();
        return PromocaoDto.de(p);
    }

    public PromocaoDto alterarAtivo(Long id, boolean ativo) {
        Promocao p = buscar(id);
        p.setAtivo(ativo);
        publicar();
        return PromocaoDto.de(p);
    }

    public void remover(Long id) {
        repository.delete(buscar(id));
        publicar();
    }

    // ================================================================== site, robô e agendamento

    /** Promoções ativas que ainda não terminaram (site). */
    @Transactional(readOnly = true)
    public List<PromocaoDto> vigentes() {
        LocalDate hoje = relogio.agora().toLocalDate();
        return repository.findAtivasComServicos().stream()
                .filter(p -> p.vigente(hoje))
                .sorted(Comparator.comparing(Promocao::getId))
                .map(PromocaoDto::de)
                .toList();
    }

    /** Melhor promoção para o serviço na data (maior desconto). */
    @Transactional(readOnly = true)
    public Optional<Promocao> melhorPara(Long servicoId, LocalDate data) {
        return repository.findAtivasComServicos().stream()
                .filter(p -> p.valePara(servicoId, data))
                .max(Comparator.comparingInt(Promocao::getDescontoPercentual));
    }

    /** Preço com o desconto da promoção do dia (ou o preço normal). */
    @Transactional(readOnly = true)
    public BigDecimal precoCom(Servico servico, LocalDate data) {
        return melhorPara(servico.getId(), data)
                .map(p -> servico.getPreco()
                        .multiply(BigDecimal.valueOf(100L - p.getDescontoPercentual()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .orElse(servico.getPreco());
    }

    /** Texto das promoções para a tabela de valores do WhatsApp (null se não houver). */
    @Transactional(readOnly = true)
    public String resumoParaWhatsapp() {
        LocalDate hoje = relogio.agora().toLocalDate();
        List<Promocao> lista = repository.findAtivasComServicos().stream().filter(p -> p.vigente(hoje)).toList();
        if (lista.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("🔥 *Promoções*\n");
        for (Promocao p : lista) {
            sb.append("• *").append(p.getTitulo()).append("*: ").append(p.getDescontoPercentual()).append("% ");
            sb.append(p.isTodosServicos() ? "em todos os serviços"
                    : "em " + p.getServicos().stream().map(Servico::getNome).sorted().collect(Collectors.joining(", ")));
            List<Integer> dias = p.dias();
            if (!dias.isEmpty()) {
                sb.append(" (").append(dias.stream().map(d -> DIAS[d]).collect(Collectors.joining(", "))).append(")");
            }
            if (p.getDataFim() != null) {
                sb.append(", até ").append(DIA_MES.format(p.getDataFim()));
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    /** true = autorizou promoções; false = pediu para sair (respondeu SAIR). */
    public void registrarPreferencia(String telefone, boolean aceita) {
        clienteRepository.findByTelefone(Telefone.normalizar(telefone))
                .ifPresent(c -> c.setAceitaPromocoes(aceita));
    }

    // ================================================================== privados

    private void aplicar(Promocao p, PromocaoDto dto) {
        if (dto.dataInicio() != null && dto.dataFim() != null && dto.dataFim().isBefore(dto.dataInicio())) {
            throw new RegraNegocioException("A data final deve ser igual ou depois da inicial.");
        }
        boolean todos = dto.todosServicos() == null || dto.todosServicos();
        List<Servico> servicos = List.of();
        if (!todos) {
            if (dto.servicoIds() == null || dto.servicoIds().isEmpty()) {
                throw new RegraNegocioException("Escolha ao menos um serviço, ou marque “todos os serviços”.");
            }
            servicos = servicoRepository.findAllById(dto.servicoIds());
        }
        p.atualizar(dto.titulo().trim(), dto.descricao() == null || dto.descricao().isBlank() ? null : dto.descricao().trim(),
                dto.descontoPercentual(), dto.diasSemana(), dto.dataInicio(), dto.dataFim(), todos,
                new HashSet<>(servicos), dto.ativo() == null || dto.ativo());
    }

    private Promocao buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Promoção não encontrada."));
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.PROMOCOES));
    }
}
