package br.com.jofashionhair.service.admin;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.HorarioFuncionamento;
import br.com.jofashionhair.domain.HorarioTrabalho;
import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.domain.Telefone;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.HorarioFuncionamentoRepository;
import br.com.jofashionhair.repository.HorarioTrabalhoRepository;
import br.com.jofashionhair.repository.ProfissionalRepository;
import br.com.jofashionhair.repository.ServicoRepository;
import br.com.jofashionhair.service.CatalogoAlteradoEvent;
import br.com.jofashionhair.service.admin.AdminDtos.FaixaHorario;
import br.com.jofashionhair.service.admin.AdminDtos.ProfissionalAdminResponse;
import br.com.jofashionhair.service.admin.AdminDtos.ProfissionalRequest;

/**
 * Gestão de profissionais: cadastro, função/especialidade, serviços que
 * realizam (N:N) e expediente semanal.
 */
@Service
@Transactional
public class ProfissionalAdminService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ProfissionalRepository profissionalRepository;
    private final ServicoRepository servicoRepository;
    private final HorarioTrabalhoRepository horarioRepository;
    private final HorarioFuncionamentoRepository funcionamentoRepository;
    private final ApplicationEventPublisher eventos;

    public ProfissionalAdminService(ProfissionalRepository profissionalRepository,
                                    ServicoRepository servicoRepository,
                                    HorarioTrabalhoRepository horarioRepository,
                                    HorarioFuncionamentoRepository funcionamentoRepository,
                                    ApplicationEventPublisher eventos) {
        this.profissionalRepository = profissionalRepository;
        this.servicoRepository = servicoRepository;
        this.horarioRepository = horarioRepository;
        this.funcionamentoRepository = funcionamentoRepository;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<ProfissionalAdminResponse> listar() {
        return profissionalRepository.findAllComServicos().stream()
                .map(p -> resposta(p, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalAdminResponse detalhar(Long id) {
        Profissional profissional = buscarComServicos(id);
        return resposta(profissional, horarioRepository.findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(id));
    }

    public ProfissionalAdminResponse criar(ProfissionalRequest req) {
        Profissional profissional = new Profissional(req.nome().trim(), req.especialidade());
        aplicar(profissional, req);
        profissionalRepository.save(profissional);
        publicar();
        return resposta(profissional, List.of());
    }

    public ProfissionalAdminResponse atualizar(Long id, ProfissionalRequest req) {
        Profissional profissional = buscarComServicos(id);
        profissional.setNome(req.nome().trim());
        profissional.setEspecialidade(req.especialidade());
        aplicar(profissional, req);
        publicar();
        return resposta(profissional, null);
    }

    /**
     * Desativar tira a profissional do site e do robô. Agendamentos futuros
     * dela NÃO são cancelados automaticamente: confira a agenda antes.
     */
    public ProfissionalAdminResponse alterarAtivo(Long id, boolean ativo) {
        Profissional profissional = buscarComServicos(id);
        profissional.setAtivo(ativo);
        publicar();
        return resposta(profissional, null);
    }

    /** Substitui o conjunto de serviços que a profissional realiza (as "funções" dela). */
    public ProfissionalAdminResponse definirServicos(Long id, Set<Long> servicoIds) {
        Profissional profissional = buscarComServicos(id);
        List<Servico> servicos = servicoRepository.findAllById(servicoIds);
        if (servicos.size() != servicoIds.size()) {
            throw new RecursoNaoEncontradoException("Um ou mais serviços informados não existem.");
        }
        profissional.getServicos().clear();
        profissional.getServicos().addAll(servicos);
        publicar();
        return resposta(profissional, null);
    }

    /**
     * Substitui a grade semanal inteira da profissional. Cada faixa precisa
     * caber dentro do horário de funcionamento do salão naquele dia.
     * Agendamentos já feitos não são alterados.
     */
    public ProfissionalAdminResponse definirHorarios(Long id, List<FaixaHorario> faixas) {
        Profissional profissional = buscarComServicos(id);
        validarFaixas(faixas);

        horarioRepository.apagarDaProfissional(id);
        List<HorarioTrabalho> novas = faixas.stream()
                .map(f -> new HorarioTrabalho(profissional, DayOfWeek.of(f.diaSemana()), f.horaInicio(), f.horaFim()))
                .toList();
        horarioRepository.saveAll(novas);

        publicar();
        List<HorarioTrabalho> ordenadas = new ArrayList<>(novas);
        ordenadas.sort(Comparator.comparing(HorarioTrabalho::getDiaSemana)
                .thenComparing(HorarioTrabalho::getHoraInicio));
        return resposta(profissional, ordenadas);
    }

    // ------------------------------------------------------------------ privados

    private void aplicar(Profissional p, ProfissionalRequest req) {
        p.setTelefone(req.telefone() == null || req.telefone().isBlank() ? null : Telefone.normalizar(req.telefone()));
        p.setBio(req.bio() == null || req.bio().isBlank() ? null : req.bio().trim());
        p.setFotoUrl(req.fotoUrl() == null || req.fotoUrl().isBlank() ? null : req.fotoUrl().trim());
    }

    private Profissional buscarComServicos(Long id) {
        return profissionalRepository.findByIdComServicos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Profissional não encontrada."));
    }

    private void validarFaixas(List<FaixaHorario> faixas) {
        Map<Short, HorarioFuncionamento> salao = funcionamentoRepository.findAll().stream()
                .collect(Collectors.toMap(HorarioFuncionamento::getDiaSemana, Function.identity()));

        for (FaixaHorario f : faixas) {
            String dia = nomeDia(f.diaSemana());
            if (!f.horaFim().isAfter(f.horaInicio())) {
                throw new RegraNegocioException(dia + ": o fim do expediente deve ser depois do início.");
            }
            HorarioFuncionamento funcionamento = salao.get(f.diaSemana().shortValue());
            if (funcionamento == null || funcionamento.isFechado()) {
                throw new RegraNegocioException(dia + ": o salão está fechado neste dia. "
                        + "Ajuste o horário de funcionamento do salão antes.");
            }
            if (!funcionamento.cobre(f.horaInicio(), f.horaFim())) {
                throw new RegraNegocioException(dia + ": o expediente precisa ficar dentro do funcionamento do salão ("
                        + funcionamento.getHoraAbertura() + " às " + funcionamento.getHoraFechamento() + ").");
            }
        }

        Map<Integer, List<FaixaHorario>> porDia = faixas.stream()
                .collect(Collectors.groupingBy(FaixaHorario::diaSemana));
        porDia.forEach((dia, lista) -> {
            List<FaixaHorario> ordenadas = new ArrayList<>(lista);
            ordenadas.sort(Comparator.comparing(FaixaHorario::horaInicio));
            for (int i = 1; i < ordenadas.size(); i++) {
                if (ordenadas.get(i).horaInicio().isBefore(ordenadas.get(i - 1).horaFim())) {
                    throw new RegraNegocioException(nomeDia(dia) + ": há faixas de horário sobrepostas.");
                }
            }
        });
    }

    /** "Segunda-feira", "Sábado"... */
    private String nomeDia(int diaSemana) {
        String nome = DayOfWeek.of(diaSemana).getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1);
    }

    private ProfissionalAdminResponse resposta(Profissional p, List<HorarioTrabalho> horarios) {
        List<Long> servicoIds = p.getServicos().stream().map(Servico::getId).sorted().toList();
        List<FaixaHorario> faixas = horarios == null ? null : horarios.stream().map(FaixaHorario::de).toList();
        return new ProfissionalAdminResponse(p.getId(), p.getNome(), p.getEspecialidade(),
                p.getTelefone(), p.getBio(), p.getFotoUrl(), p.isAtivo(), servicoIds, faixas);
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.PROFISSIONAIS));
    }
}
