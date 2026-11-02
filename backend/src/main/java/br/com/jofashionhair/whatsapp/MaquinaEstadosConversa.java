package br.com.jofashionhair.whatsapp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import br.com.jofashionhair.config.AppProperties;
import br.com.jofashionhair.domain.AtendimentoWhatsappEstado;
import br.com.jofashionhair.domain.ContextoConversa;
import br.com.jofashionhair.domain.ContextoConversa.OpcaoHorario;
import br.com.jofashionhair.domain.EstadoConversa;
import br.com.jofashionhair.domain.OrigemAgendamento;
import br.com.jofashionhair.domain.PerguntaFrequente;
import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.Servico;
import br.com.jofashionhair.domain.StatusAgendamento;
import br.com.jofashionhair.exception.HorarioIndisponivelException;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.repository.ProfissionalRepository;
import br.com.jofashionhair.repository.ServicoRepository;
import br.com.jofashionhair.service.AgendamentoResumo;
import br.com.jofashionhair.service.AgendamentoService;
import br.com.jofashionhair.service.DisponibilidadeService;
import br.com.jofashionhair.service.HorariosProfissional;
import br.com.jofashionhair.service.RelogioSalao;
import br.com.jofashionhair.service.SolicitacaoAgendamento;
import br.com.jofashionhair.service.SugestaoHorario;
import br.com.jofashionhair.whatsapp.InterpretadorMensagem.DataInterpretada;

/**
 * Máquina de estados da conversa no WhatsApp.
 *
 * <p>Funciona como "preenchimento de lacunas": a cada mensagem extrai tudo o
 * que conseguir (serviço, profissional, data, hora) e pergunta apenas o que
 * ainda falta, na ordem serviço → profissional → dia/horário. O estado atual
 * serve para interpretar respostas numéricas ("2") do menu enviado.</p>
 *
 * <p>Serviços, preços, durações, profissionais, funcionamento do salão e
 * expedientes são lidos do banco A CADA mensagem: qualquer alteração feita no
 * /admin vale imediatamente para o robô.</p>
 *
 * <p>Esta classe NÃO é transacional de propósito: a reserva roda na transação
 * própria do AgendamentoService, e uma recusa ("horário ocupado") não
 * contamina a gravação do estado da conversa.</p>
 */
@Component
public class MaquinaEstadosConversa {

    private static final int MAX_OPCOES_HORARIO = 8;
    private static final int MAX_SUGESTOES = 6;
    private static final int MAX_TAMANHO_NOME = 120;

    private final InterpretadorMensagem interpretador;
    private final MensagensBot mensagens;
    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final DisponibilidadeService disponibilidade;
    private final AgendamentoService agendamentoService;
    private final RelogioSalao relogio;
    private final AppProperties props;
    private final RespostasFrequentes respostas;
    private final br.com.jofashionhair.promocao.PromocaoService promocaoService;
    private final br.com.jofashionhair.service.FuncionamentoService funcionamento;
    private final br.com.jofashionhair.pagamento.SinalService sinalService;
    private final br.com.jofashionhair.pagamento.PixService pixService;

    public MaquinaEstadosConversa(InterpretadorMensagem interpretador,
                                  MensagensBot mensagens,
                                  ServicoRepository servicoRepository,
                                  ProfissionalRepository profissionalRepository,
                                  DisponibilidadeService disponibilidade,
                                  AgendamentoService agendamentoService,
                                  RelogioSalao relogio,
                                  AppProperties props,
                                  RespostasFrequentes respostas,
                                  br.com.jofashionhair.promocao.PromocaoService promocaoService,
                                  br.com.jofashionhair.service.FuncionamentoService funcionamento,
                                  br.com.jofashionhair.pagamento.SinalService sinalService,
                                  br.com.jofashionhair.pagamento.PixService pixService) {
        this.interpretador = interpretador;
        this.mensagens = mensagens;
        this.servicoRepository = servicoRepository;
        this.profissionalRepository = profissionalRepository;
        this.disponibilidade = disponibilidade;
        this.agendamentoService = agendamentoService;
        this.relogio = relogio;
        this.props = props;
        this.respostas = respostas;
        this.promocaoService = promocaoService;
        this.funcionamento = funcionamento;
        this.sinalService = sinalService;
        this.pixService = pixService;
    }

    /**
     * Processa uma mensagem e devolve a resposta (ou null para não responder,
     * ex.: conversa em atendimento humano). Altera {@code conversa}; quem chama
     * é responsável por gravá-la.
     */
    public String processar(AtendimentoWhatsappEstado conversa, String nomeContato, String texto) {
        OffsetDateTime agora = relogio.agoraOffset();
        String n = interpretador.normalizar(texto);
        boolean expirada = conversa.expirada(agora);

        // Atendimento humano: o robô fica em silêncio até a pausa expirar ou o cliente mandar "menu"
        if (conversa.getEstado() == EstadoConversa.ATENDIMENTO_HUMANO) {
            if (!expirada && !interpretador.ehMenu(n)) {
                return null;
            }
            conversa.reiniciar();
            expirada = true;
        }

        boolean nova = conversa.getId() == null || expirada || conversa.getEstado() == EstadoConversa.INICIAL;
        if (expirada) {
            conversa.reiniciar();
        }
        conversa.registrarInteracao(agora, props.whatsapp().expiracaoConversaHoras());

        if (n.isBlank()) {
            return mensagens.soTexto();
        }

        // ---------------------------------------------------------- comandos globais
        // "já paguei" com um sinal em aberto
        if (interpretador.avisaPagamento(n)
                && agendamentoService.futurosDoCliente(conversa.getTelefone()).stream()
                        .anyMatch(a -> a.status() == StatusAgendamento.AGUARDANDO_SINAL)) {
            return mensagens.pagamentoEmConferencia(pixService.automatico());
        }
        if (interpretador.pedeSairPromocoes(n)) {
            promocaoService.registrarPreferencia(conversa.getTelefone(), false);
            conversa.finalizar();
            return mensagens.saiuPromocoes();
        }
        if (interpretador.pedeAtendente(n)) {
            return encaminharParaAtendente(conversa, agora);
        }
        if (interpretador.ehMenu(n)) {
            conversa.reiniciar();
            return saudarEPedirServico(conversa, nomeContato);
        }
        if (!nova) {
            String resposta = switch (conversa.getEstado()) {
                case AGUARDANDO_CONFIRMACAO_LEMBRETE -> tratarRespostaLembrete(conversa, n);
                case AGUARDANDO_ESCOLHA_CANCELAMENTO -> tratarEscolhaCancelamento(conversa, n);
                default -> null;
            };
            if (resposta != null) {
                return resposta;
            }
        }
        if (interpretador.pedeCancelamento(n)) {
            return iniciarCancelamento(conversa);
        }
        if (conversa.getEstado() == EstadoConversa.FINALIZADO) {
            if (interpretador.ehAgradecimento(n)) {
                return mensagens.deNada();
            }
            conversa.reiniciar();   // novo pedido depois de um agendamento concluído
            nova = true;
        }

        // ---------------------------------------------------------- fluxo de agendamento
        String prefixo = nova ? mensagens.saudacao(nomeContato) : "";
        List<Servico> catalogo = servicoRepository.findAtivosComProfissional();

        // "tabela", "valores", "quanto custa"... (sem citar um serviço específico)
        if (interpretador.pedeTabela(n) && interpretador.encontrarServicos(n, catalogo).isEmpty()) {
            ContextoConversa ctx = conversa.getContexto();
            if (ctx.getServicoId() == null) {
                ctx.setOpcoesServicos(catalogo.stream().map(Servico::getId).toList());
                conversa.setEstado(EstadoConversa.AGUARDANDO_SERVICO);
            }
            return prefixo + mensagens.tabelaValores(catalogo, promocaoService.resumoParaWhatsapp());
        }

        // Pergunta frequente cadastrada no painel ("vocês aceitam pix?")
        Optional<PerguntaFrequente> frequente = respostas.encontrar(n);
        if (frequente.isPresent() && interpretador.ehPergunta(n)) {
            return prefixo + responderFrequente(conversa, frequente.get(), nomeContato);
        }

        Interpretacao interpretacao = interpretar(conversa, n);
        if (interpretacao.duvida() != null) {
            return prefixo + interpretacao.duvida();
        }
        ContextoConversa ctx = conversa.getContexto();

        if (!interpretacao.entendeu()) {
            // só um "oi" numa conversa nova: a saudação já basta
            if (nova && interpretador.ehSaudacao(n)) {
                return saudarEPedirServico(conversa, nomeContato);
            }
            // palavras-chave de uma pergunta frequente, mesmo sem "?"
            if (frequente.isPresent()) {
                return prefixo + responderFrequente(conversa, frequente.get(), nomeContato);
            }
            // não entendeu: pede para reformular; na segunda vez, chama a equipe
            ctx.setTentativasInvalidas(ctx.getTentativasInvalidas() + 1);
            if (ctx.getTentativasInvalidas() >= 2) {
                return prefixo + encaminharParaAtendente(conversa, agora);
            }
            return prefixo + mensagens.naoEntendi(1) + avancar(conversa, interpretacao.candidatosServico(), nomeContato);
        }

        ctx.setTentativasInvalidas(0);
        // conversa nova sem serviço citado: a saudação já pergunta o serviço
        if (nova && ctx.getServicoId() == null && interpretacao.candidatosServico().isEmpty()) {
            return saudarEPedirServico(conversa, nomeContato);
        }
        return prefixo + avancar(conversa, interpretacao.candidatosServico(), nomeContato);
    }

    /** Resposta cadastrada no painel; no meio de um agendamento, retoma de onde parou. */
    private String responderFrequente(AtendimentoWhatsappEstado conversa, PerguntaFrequente frequente,
                                      String nomeContato) {
        ContextoConversa ctx = conversa.getContexto();
        ctx.setTentativasInvalidas(0);
        if (ctx.getServicoId() == null) {
            ctx.setOpcoesServicos(servicoRepository.findAtivosComProfissional().stream().map(Servico::getId).toList());
            conversa.setEstado(EstadoConversa.AGUARDANDO_SERVICO);
            return frequente.getResposta();
        }
        return frequente.getResposta() + "\n\n" + avancar(conversa, List.of(), nomeContato);
    }

    /** Saudação do salão; a próxima mensagem da cliente é o serviço. */
    private String saudarEPedirServico(AtendimentoWhatsappEstado conversa, String nomeContato) {
        conversa.getContexto().setOpcoesServicos(
                servicoRepository.findAtivosComProfissional().stream().map(Servico::getId).toList());
        conversa.setEstado(EstadoConversa.AGUARDANDO_SERVICO);
        return mensagens.saudacao(nomeContato).stripTrailing();
    }

    /** "Alguém já irá te atender": o robô pausa e a conversa vai para "Pediram atendente". */
    private String encaminharParaAtendente(AtendimentoWhatsappEstado conversa, OffsetDateTime agora) {
        // Fora do horário NÃO chama ninguém: avisa que está fechado e o robô segue agendando
        if (!funcionamento.abertoAgora()) {
            conversa.getContexto().setTentativasInvalidas(0);
            return mensagens.fechadoAgora(funcionamento.proximaAbertura());
        }
        conversa.pausarParaAtendimentoHumano(agora.plusHours(props.whatsapp().pausaAtendimentoHumanoHoras()));
        return mensagens.encaminhadoAtendente();
    }

    // ===================================================================== interpretação

    private Interpretacao interpretar(AtendimentoWhatsappEstado conversa, String n) {
        ContextoConversa ctx = conversa.getContexto();
        EstadoConversa estado = conversa.getEstado();
        boolean entendeu = false;

        // 1) Resposta numérica ao menu que foi enviado
        Integer opcao = interpretador.extrairOpcao(n);
        if (opcao != null) {
            if (aplicarOpcao(conversa, opcao)) {
                return Interpretacao.entendida(List.of());
            }
            // nas etapas de horário, "17" sozinho fora do menu é uma hora
            boolean etapaHorario = estado == EstadoConversa.AGUARDANDO_DATA_HORA
                    || estado == EstadoConversa.AGUARDANDO_ESCOLHA_HORARIO;
            if (etapaHorario && opcao >= 6 && opcao <= 22) {
                ctx.definirHora(LocalTime.of(opcao, 0));
                return Interpretacao.entendida(List.of());
            }
            return Interpretacao.naoEntendida();
        }

        // 2) Data e hora
        DataInterpretada data = interpretador.extrairData(n, relogio.agora().toLocalDate());
        Optional<LocalTime> hora = interpretador.extrairHora(n);
        if (hora.isPresent()) {
            ctx.definirHora(hora.get());
            entendeu = true;
        }
        if (data.duvida() != null) {
            conversa.setEstado(EstadoConversa.AGUARDANDO_DATA_HORA);
            return Interpretacao.comDuvida(data.duvida());
        }
        if (data.data() != null) {
            ctx.definirData(data.data());
            entendeu = true;
        }

        // 3) Serviço (catálogo atual do banco)
        List<Servico> candidatos = List.of();
        List<Servico> encontrados = interpretador.encontrarServicos(n, servicoRepository.findAtivosComProfissional());
        if (ctx.getServicoId() == null) {
            if (encontrados.size() == 1) {
                ctx.escolherServico(encontrados.get(0).getId());
                entendeu = true;
            } else if (encontrados.size() > 1) {
                candidatos = encontrados;
                entendeu = true;
            }
        } else if (encontrados.size() == 1
                && !encontrados.get(0).getId().equals(ctx.getServicoId())
                && interpretador.mencionaNomeCompleto(n, encontrados.get(0).getNome())) {
            ctx.escolherServico(encontrados.get(0).getId());   // cliente mudou de ideia
            entendeu = true;
        }

        // 4) Profissional (só depois de saber o serviço)
        if (ctx.getServicoId() != null && !ctx.profissionalDecidido()) {
            if (interpretador.ehSemPreferencia(n)) {
                ctx.escolherSemPreferencia();
                entendeu = true;
            } else {
                Optional<Profissional> profissional = interpretador.encontrarProfissional(n,
                        profissionalRepository.findAtivasQueRealizam(ctx.getServicoId()));
                if (profissional.isPresent()) {
                    ctx.escolherProfissional(profissional.get().getId());
                    entendeu = true;
                }
            }
        }
        return entendeu ? Interpretacao.entendida(candidatos) : Interpretacao.naoEntendida();
    }

    /** Aplica "2" ao menu correspondente ao estado atual. */
    private boolean aplicarOpcao(AtendimentoWhatsappEstado conversa, int opcao) {
        ContextoConversa ctx = conversa.getContexto();
        return switch (conversa.getEstado()) {
            case AGUARDANDO_SERVICO -> {
                Long id = item(ctx.getOpcoesServicos(), opcao);
                if (id == null) {
                    yield false;
                }
                ctx.escolherServico(id);
                yield true;
            }
            case AGUARDANDO_PROFISSIONAL -> {
                if (opcao == 1) {
                    ctx.escolherSemPreferencia();
                    yield true;
                }
                Long id = item(ctx.getOpcoesProfissionais(), opcao - 1);
                if (id == null) {
                    yield false;
                }
                ctx.escolherProfissional(id);
                yield true;
            }
            case AGUARDANDO_ESCOLHA_HORARIO -> {
                OpcaoHorario escolhida = item(ctx.getOpcoesHorarios(), opcao);
                if (escolhida == null) {
                    yield false;
                }
                ctx.escolherProfissional(escolhida.getProfissionalId());
                ctx.definirData(escolhida.inicioLocal().toLocalDate());
                ctx.definirHora(escolhida.inicioLocal().toLocalTime());
                yield true;
            }
            default -> false;
        };
    }

    // ===================================================================== próximo passo

    /** Pergunta o que falta ou, se já tem tudo, tenta reservar. */
    private String avancar(AtendimentoWhatsappEstado conversa, List<Servico> candidatos, String nomeContato) {
        ContextoConversa ctx = conversa.getContexto();

        // ---- Serviço
        if (ctx.getServicoId() == null) {
            List<Servico> lista = candidatos.isEmpty() ? servicoRepository.findAtivosComProfissional() : candidatos;
            if (lista.isEmpty()) {
                conversa.finalizar();
                return mensagens.semServicos();
            }
            ctx.setOpcoesServicos(lista.stream().map(Servico::getId).toList());
            conversa.setEstado(EstadoConversa.AGUARDANDO_SERVICO);
            return mensagens.perguntarServico(lista, !candidatos.isEmpty());
        }
        Servico servico = servicoRepository.findById(ctx.getServicoId()).filter(Servico::isAtivo).orElse(null);
        if (servico == null) {
            ctx.limparServico();
            return mensagens.servicoIndisponivel() + avancar(conversa, List.of(), nomeContato);
        }

        // ---- Profissional
        if (!ctx.profissionalDecidido()) {
            List<Profissional> profissionais = profissionalRepository.findAtivasQueRealizam(servico.getId());
            if (profissionais.isEmpty()) {
                ctx.limparServico();
                return mensagens.semProfissional(servico) + avancar(conversa, List.of(), nomeContato);
            }
            if (profissionais.size() == 1) {
                ctx.escolherProfissional(profissionais.get(0).getId());   // só uma faz: não precisa perguntar
            } else {
                ctx.setOpcoesProfissionais(profissionais.stream().map(Profissional::getId).toList());
                conversa.setEstado(EstadoConversa.AGUARDANDO_PROFISSIONAL);
                return mensagens.perguntarProfissional(servico, profissionais);
            }
        }

        // ---- Dia e horário
        if (ctx.dataLocal() == null) {
            conversa.setEstado(EstadoConversa.AGUARDANDO_DATA_HORA);
            return mensagens.perguntarDataHora(servico, ctx.horaLocal());
        }
        if (ctx.horaLocal() == null) {
            return oferecerHorariosDoDia(conversa, servico);
        }
        return tentarReservar(conversa, servico, nomeContato);
    }

    /** Cliente disse o dia mas não a hora: mostra os horários livres daquele dia. */
    private String oferecerHorariosDoDia(AtendimentoWhatsappEstado conversa, Servico servico) {
        ContextoConversa ctx = conversa.getContexto();
        LocalDate data = ctx.dataLocal();
        Long profissionalId = ctx.isSemPreferencia() ? null : ctx.getProfissionalId();

        // um horário por início; se várias estão livres no mesmo horário, fica a primeira
        Map<LocalDateTime, SugestaoHorario> porInicio = new TreeMap<>();
        for (HorariosProfissional hp : disponibilidade.horariosLivresDoDia(servico.getId(), profissionalId, data)) {
            for (LocalTime h : hp.horarios()) {
                porInicio.putIfAbsent(data.atTime(h),
                        new SugestaoHorario(hp.profissionalId(), hp.profissionalNome(), data.atTime(h)));
            }
        }
        List<SugestaoHorario> opcoes = distribuir(new ArrayList<>(porInicio.values()), MAX_OPCOES_HORARIO);
        if (!opcoes.isEmpty()) {
            guardarOpcoesHorario(conversa, opcoes);
            return mensagens.horariosDoDia(data, opcoes);
        }

        List<SugestaoHorario> sugestoes = disponibilidade.sugerirAlternativas(
                servico.getId(), profissionalId, data.atTime(LocalTime.NOON), MAX_SUGESTOES);
        ctx.definirData(null);
        if (sugestoes.isEmpty()) {
            conversa.setEstado(EstadoConversa.AGUARDANDO_DATA_HORA);
            return mensagens.semHorariosProximos(data);
        }
        guardarOpcoesHorario(conversa, sugestoes);
        return mensagens.semHorariosNoDia(data, sugestoes);
    }

    /**
     * Valida e grava de forma atômica (lock da profissional + constraint do banco).
     * Livre → CONFIRMADO. Ocupado → oferece alternativas.
     */
    private String tentarReservar(AtendimentoWhatsappEstado conversa, Servico servico, String nomeContato) {
        ContextoConversa ctx = conversa.getContexto();
        LocalDateTime inicio = ctx.dataLocal().atTime(ctx.horaLocal());
        Long profissionalId = ctx.isSemPreferencia() ? null : ctx.getProfissionalId();
        try {
            AgendamentoResumo reserva = agendamentoService.reservar(new SolicitacaoAgendamento(
                    servico.getId(), profissionalId, inicio, nomeCliente(nomeContato),
                    conversa.getTelefone(), OrigemAgendamento.WHATSAPP, null));
            conversa.finalizar();
            if (reserva.status() == StatusAgendamento.AGUARDANDO_SINAL) {
                try {
                    var sinal = sinalService.iniciar(reserva.id());
                    long minutos = Math.max(1, java.time.Duration.between(relogio.agora(), sinal.expiraEm()).toMinutes());
                    return mensagens.sinalPendente(reserva, sinal, minutos);
                } catch (br.com.jofashionhair.exception.RegraNegocioException e) {
                    return e.getMessage();
                }
            }
            return mensagens.confirmado(reserva);

        } catch (HorarioIndisponivelException e) {
            ctx.definirHora(null);   // a próxima hora informada dispara nova tentativa
            List<SugestaoHorario> sugestoes = disponibilidade.sugerirAlternativas(
                    servico.getId(), profissionalId, inicio, MAX_SUGESTOES);
            if (sugestoes.isEmpty()) {
                conversa.setEstado(EstadoConversa.AGUARDANDO_DATA_HORA);
                return mensagens.indisponivelSemSugestoes(e.getMotivo());
            }
            guardarOpcoesHorario(conversa, sugestoes);
            return mensagens.indisponivel(e.getMotivo(), sugestoes);

        } catch (RecursoNaoEncontradoException e) {
            // serviço ou profissional desativados no meio da conversa
            ctx.limparServico();
            return mensagens.servicoIndisponivel() + avancar(conversa, List.of(), nomeContato);
        }
    }

    // ===================================================================== cancelamento e lembrete

    private String iniciarCancelamento(AtendimentoWhatsappEstado conversa) {
        List<AgendamentoResumo> futuros = agendamentoService.futurosDoCliente(conversa.getTelefone());
        if (futuros.isEmpty()) {
            conversa.finalizar();
            return mensagens.semAgendamentosFuturos();
        }
        conversa.reiniciar();
        conversa.getContexto().setOpcoesAgendamentos(futuros.stream().map(AgendamentoResumo::id).toList());
        conversa.setEstado(EstadoConversa.AGUARDANDO_ESCOLHA_CANCELAMENTO);
        return mensagens.escolherCancelamento(futuros);
    }

    /** @return resposta, ou null se a mensagem não era sobre o cancelamento */
    private String tratarEscolhaCancelamento(AtendimentoWhatsappEstado conversa, String n) {
        Integer opcao = interpretador.extrairOpcao(n);
        Long id = opcao == null ? null : item(conversa.getContexto().getOpcoesAgendamentos(), opcao);
        if (id != null) {
            try {
                AgendamentoResumo cancelado = agendamentoService.cancelarPeloCliente(id, conversa.getTelefone());
                conversa.prepararRemarcacao(cancelado.servicoId(), cancelado.profissionalId());
                return mensagens.cancelado(cancelado);
            } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
                conversa.finalizar();
                return mensagens.agendamentoInativo();
            }
        }
        if (interpretador.ehNao(n) && !interpretador.pedeCancelamento(n)) {
            conversa.finalizar();
            return mensagens.nadaCancelado();
        }
        return null;
    }

    /**
     * Resposta ao lembrete do Passo 5 ("1 - Confirmar | 2 - Cancelar").
     * @return resposta, ou null se a mensagem não era sobre o lembrete
     */
    private String tratarRespostaLembrete(AtendimentoWhatsappEstado conversa, String n) {
        Long agendamentoId = conversa.getContexto().getAgendamentoLembreteId();
        if (agendamentoId == null) {
            return null;
        }
        Integer opcao = interpretador.extrairOpcao(n);
        try {
            if (Integer.valueOf(1).equals(opcao) || interpretador.ehSim(n)) {
                AgendamentoResumo r = agendamentoService.confirmarPeloCliente(agendamentoId, conversa.getTelefone());
                conversa.finalizar();
                return mensagens.presencaConfirmada(r);
            }
            if (Integer.valueOf(2).equals(opcao) || interpretador.ehNao(n)) {
                AgendamentoResumo r = agendamentoService.cancelarPeloCliente(agendamentoId, conversa.getTelefone());
                conversa.prepararRemarcacao(r.servicoId(), r.profissionalId());
                return mensagens.cancelado(r);
            }
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            conversa.finalizar();
            return mensagens.agendamentoInativo();
        }
        if (opcao != null) {
            return mensagens.respostaLembreteNaoEntendida();
        }
        return null;   // outro assunto: segue o fluxo normal
    }

    // ===================================================================== auxiliares

    private void guardarOpcoesHorario(AtendimentoWhatsappEstado conversa, List<SugestaoHorario> opcoes) {
        conversa.getContexto().setOpcoesHorarios(opcoes.stream()
                .map(s -> new OpcaoHorario(s.profissionalId(), s.inicio()))
                .toList());
        conversa.setEstado(EstadoConversa.AGUARDANDO_ESCOLHA_HORARIO);
    }

    /** Escolhe até {@code max} itens espalhados pela lista (manhã, tarde e noite). */
    private static <T> List<T> distribuir(List<T> lista, int max) {
        if (lista.size() <= max) {
            return lista;
        }
        List<T> resultado = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            resultado.add(lista.get((int) Math.round(i * (lista.size() - 1) / (double) (max - 1))));
        }
        return resultado;
    }

    private static <T> T item(List<T> lista, int posicao) {
        return posicao >= 1 && posicao <= lista.size() ? lista.get(posicao - 1) : null;
    }

    private static String nomeCliente(String nomeContato) {
        if (nomeContato == null || nomeContato.isBlank()) {
            return "Cliente WhatsApp";
        }
        String nome = nomeContato.trim();
        return nome.length() > MAX_TAMANHO_NOME ? nome.substring(0, MAX_TAMANHO_NOME) : nome;
    }

    private record Interpretacao(boolean entendeu, String duvida, List<Servico> candidatosServico) {

        static Interpretacao entendida(List<Servico> candidatos) {
            return new Interpretacao(true, null, candidatos);
        }

        static Interpretacao naoEntendida() {
            return new Interpretacao(false, null, List.of());
        }

        static Interpretacao comDuvida(String duvida) {
            return new Interpretacao(true, duvida, List.of());
        }
    }
}
