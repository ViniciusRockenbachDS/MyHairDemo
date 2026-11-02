package br.com.jofashionhair.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.ConfiguracaoSalao;
import br.com.jofashionhair.domain.HorarioFuncionamento;
import br.com.jofashionhair.domain.Telefone;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.ConfiguracaoSalaoRepository;
import br.com.jofashionhair.repository.HorarioFuncionamentoRepository;

/**
 * Dados do salão: informações institucionais e horário de funcionamento.
 * Toda alteração dispara o evento SSE "salao": o site recarrega sozinho, e o
 * robô usa o valor novo já na próxima mensagem.
 */
@Service
@Transactional
public class SalaoService {

    /** Tudo o que o site precisa sobre o salão numa única chamada. */
    public record SalaoPublico(SalaoInformacoesDto informacoes, List<HorarioFuncionamentoDto> horarios) {
    }

    private final HorarioFuncionamentoRepository horarioRepository;
    private final ConfiguracaoSalaoRepository configuracaoRepository;
    private final ApplicationEventPublisher eventos;

    public SalaoService(HorarioFuncionamentoRepository horarioRepository,
                        ConfiguracaoSalaoRepository configuracaoRepository,
                        ApplicationEventPublisher eventos) {
        this.horarioRepository = horarioRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.eventos = eventos;
    }

    // ------------------------------------------------------------------ informações

    @Transactional(readOnly = true)
    public SalaoInformacoesDto informacoes() {
        return SalaoInformacoesDto.de(configuracao());
    }

    @Transactional(readOnly = true)
    public SalaoPublico dadosPublicos() {
        return new SalaoPublico(informacoes(), listarHorarios());
    }

    public SalaoInformacoesDto atualizarInformacoes(SalaoInformacoesDto dto) {
        ConfiguracaoSalao c = configuracao();
        c.atualizar(dto.nome().trim(), vazioParaNull(dto.slogan()), vazioParaNull(dto.descricao()),
                vazioParaNull(dto.endereco()), vazioParaNull(dto.cidade()),
                telefoneOuNull(dto.telefone()), telefoneOuNull(dto.whatsapp()),
                dto.instagram() == null ? null : vazioParaNull(dto.instagram().replace("@", "")),
                vazioParaNull(dto.linkMapa()));
        c.atualizarImagens(vazioParaNull(dto.logoUrl()), vazioParaNull(dto.fotoEspacoUrl()),
                vazioParaNull(dto.fotoVisitaUrl()));
        c.atualizarPrivacidade(vazioParaNull(dto.razaoSocial()), vazioParaNull(dto.cnpj()),
                vazioParaNull(dto.emailPrivacidade()), vazioParaNull(dto.politicaPrivacidade()),
                dto.retencaoMeses() == null ? 24 : dto.retencaoMeses());
        c.setInfoAtendimento(vazioParaNull(dto.infoAtendimento()));
        c.setFotoHeroUrl(vazioParaNull(dto.fotoHeroUrl()));
        c.definirPix(vazioParaNull(dto.chavePix()), vazioParaNull(dto.pixNomeRecebedor()),
                vazioParaNull(dto.pixCidade()), dto.sinalPrazoMinutos());
        publicar();
        return SalaoInformacoesDto.de(c);
    }

    // ------------------------------------------------------------------ funcionamento

    @Transactional(readOnly = true)
    public List<HorarioFuncionamentoDto> listarHorarios() {
        return horarioRepository.findAllByOrderByDiaSemanaAsc().stream().map(HorarioFuncionamentoDto::de).toList();
    }

    /**
     * Atualiza os dias enviados (não precisa mandar a semana toda).
     * Agendamentos já feitos não são alterados.
     */
    public List<HorarioFuncionamentoDto> definirHorarios(List<HorarioFuncionamentoDto> dias) {
        for (HorarioFuncionamentoDto dia : dias) {
            if (!dia.fechado()) {
                if (dia.abertura() == null || dia.fechamento() == null) {
                    throw new RegraNegocioException("Informe abertura e fechamento dos dias em que o salão abre.");
                }
                if (!dia.fechamento().isAfter(dia.abertura())) {
                    throw new RegraNegocioException("O fechamento deve ser depois da abertura.");
                }
            }
            HorarioFuncionamento horario = horarioRepository.findById(dia.diaSemana().shortValue())
                    .orElseGet(() -> new HorarioFuncionamento(dia.diaSemana()));
            horario.atualizar(dia.fechado(), dia.abertura(), dia.fechamento());
            horarioRepository.save(horario);
        }
        publicar();
        return listarHorarios();
    }

    // ------------------------------------------------------------------ privados

    private ConfiguracaoSalao configuracao() {
        return configuracaoRepository.findById(ConfiguracaoSalao.ID_UNICO)
                .orElseThrow(() -> new IllegalStateException("Configuração do salão ausente (migration V3)."));
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.SALAO));
    }

    private static String vazioParaNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String telefoneOuNull(String s) {
        return vazioParaNull(s) == null ? null : Telefone.normalizar(s);
    }
}
