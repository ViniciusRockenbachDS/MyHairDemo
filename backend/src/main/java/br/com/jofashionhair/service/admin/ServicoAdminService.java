package br.com.jofashionhair.service.admin;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.ServicoRepository;
import br.com.jofashionhair.service.CatalogoAlteradoEvent;
import br.com.jofashionhair.service.admin.AdminDtos.PrecoDuracaoRequest;
import br.com.jofashionhair.service.admin.AdminDtos.ServicoAdminResponse;
import br.com.jofashionhair.service.admin.AdminDtos.ServicoRequest;

/**
 * Gestão de serviços, preços e durações.
 *
 * <p><b>Sincronização:</b> não existe cache. Site e robô leem preço/duração
 * do banco a cada consulta, então o novo valor vale a partir do commit. Além
 * disso, um evento SSE avisa os navegadores abertos para recarregarem.</p>
 *
 * <p><b>Agendamentos já feitos</b> mantêm o preço e a duração do momento da
 * reserva (snapshot): uma alteração aqui nunca "desloca" a agenda existente.</p>
 */
@Service
@Transactional
public class ServicoAdminService {

    private final br.com.jofashionhair.pagamento.PixService pixService;

    private final ServicoRepository servicoRepository;
    private final ApplicationEventPublisher eventos;

    public ServicoAdminService(ServicoRepository servicoRepository, ApplicationEventPublisher eventos,
                               br.com.jofashionhair.pagamento.PixService pixService) {
        this.pixService = pixService;
        this.servicoRepository = servicoRepository;
        this.eventos = eventos;
    }

    /** Todos os serviços: ativos primeiro, depois pela ordem de exibição. */
    @Transactional(readOnly = true)
    public List<ServicoAdminResponse> listar() {
        Sort ordem = Sort.by(Sort.Order.desc("ativo"), Sort.Order.asc("ordemExibicao"), Sort.Order.asc("nome"));
        return servicoRepository.findAll(ordem).stream().map(ServicoAdminResponse::de).toList();
    }

    public ServicoAdminResponse criar(ServicoRequest req) {
        validarNomeUnico(req.nome(), null);
        Servico servico = new Servico(req.nome().trim(), req.categoria(), req.preco(), req.duracaoMinutos());
        aplicar(servico, req);
        servicoRepository.save(servico);
        publicar();
        return ServicoAdminResponse.de(servico);
    }

    public ServicoAdminResponse atualizar(Long id, ServicoRequest req) {
        Servico servico = buscar(id);
        validarNomeUnico(req.nome(), id);
        aplicar(servico, req);
        publicar();
        return ServicoAdminResponse.de(servico);
    }

    /** Edição rápida de preço e/ou duração. */
    public ServicoAdminResponse atualizarPrecoDuracao(Long id, PrecoDuracaoRequest req) {
        if (req.preco() == null && req.duracaoMinutos() == null) {
            throw new RegraNegocioException("Informe o novo preço e/ou a nova duração.");
        }
        Servico servico = buscar(id);
        if (req.preco() != null) {
            servico.setPreco(req.preco());
        }
        if (req.duracaoMinutos() != null) {
            servico.setDuracaoMinutos(req.duracaoMinutos());
        }
        publicar();
        return ServicoAdminResponse.de(servico);
    }

    /**
     * "Remover" = desativar. O serviço some do site e do robô, mas o histórico
     * de agendamentos continua íntegro. Pode ser reativado depois.
     */
    public ServicoAdminResponse alterarAtivo(Long id, boolean ativo) {
        Servico servico = buscar(id);
        servico.setAtivo(ativo);
        publicar();
        return ServicoAdminResponse.de(servico);
    }

    // ------------------------------------------------------------------ privados

    private Servico buscar(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));
    }

    private void aplicar(Servico servico, ServicoRequest req) {
        servico.setNome(req.nome().trim());
        servico.setDescricao(req.descricao());
        servico.setCategoria(req.categoria());
        servico.setPreco(req.preco());
        servico.setDuracaoMinutos(req.duracaoMinutos());
        servico.setOrdemExibicao(req.ordemExibicao() == null ? 0 : req.ordemExibicao());
        servico.setFotoUrl(req.fotoUrl() == null || req.fotoUrl().isBlank() ? null : req.fotoUrl().trim());

        boolean exigeSinal = Boolean.TRUE.equals(req.exigeSinal());
        if (exigeSinal) {
            if (req.valorSinal() == null) {
                throw new RegraNegocioException("Informe o valor do sinal.");
            }
            if (req.valorSinal().compareTo(req.preco()) > 0) {
                throw new RegraNegocioException("O sinal não pode ser maior que o preço do serviço.");
            }
            if (!pixService.configurado()) {
                throw new RegraNegocioException(
                        "Para cobrar sinal, cadastre antes a chave Pix em “Salão e horários”.");
            }
        }
        servico.definirSinal(exigeSinal, req.valorSinal());
    }

    private void validarNomeUnico(String nome, Long idAtual) {
        String limpo = nome.trim();
        boolean existe = idAtual == null
                ? servicoRepository.existsByNomeIgnoreCase(limpo)
                : servicoRepository.existsByNomeIgnoreCaseAndIdNot(limpo, idAtual);
        if (existe) {
            throw new RegraNegocioException("Já existe um serviço chamado \"" + limpo + "\".");
        }
    }

    private void publicar() {
        eventos.publishEvent(new CatalogoAlteradoEvent(CatalogoAlteradoEvent.SERVICOS));
    }
}
