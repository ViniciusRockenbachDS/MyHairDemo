package br.com.jofashionhair.conta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.config.AppProperties;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.seguranca.LimitadorTaxa;
import br.com.jofashionhair.service.RelogioSalao;
import br.com.jofashionhair.service.SalaoService;

/**
 * Contas do painel com verificação em duas etapas por e-mail.
 *
 * <ul>
 *   <li><b>Esqueci minha senha:</b> código de 6 dígitos por e-mail (15 min) + nova senha.</li>
 *   <li><b>Trocar senha (logada):</b> pede o código no e-mail da conta e confirma com ele.</li>
 *   <li><b>Convite:</b> o dono cadastra alguém da equipe; a pessoa recebe um código
 *       (48 h) para criar a própria senha.</li>
 * </ul>
 *
 * Segurança: códigos aleatórios (SecureRandom), guardados só como hash SHA-256,
 * no máximo 5 tentativas cada, um código novo invalida o anterior, e a tela de
 * "esqueci" nunca revela se o e-mail existe.
 */
@Service
public class ContaService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int MAX_PEDIDOS_POR_HORA = 3;
    private static final String RESPOSTA_ESQUECI =
            "Se este e-mail estiver cadastrado, enviamos um código de 6 números. Ele vale por 15 minutos.";

    private final UsuarioAdminRepository usuarioRepository;
    private final CodigoVerificacaoRepository codigoRepository;
    private final DispositivoConfiavelRepository dispositivoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SalaoService salaoService;
    private final LimitadorTaxa limitador;
    private final RelogioSalao relogio;
    private final String siteUrl;

    public ContaService(UsuarioAdminRepository usuarioRepository,
                        CodigoVerificacaoRepository codigoRepository,
                        DispositivoConfiavelRepository dispositivoRepository,
                        PasswordEncoder passwordEncoder,
                        EmailService emailService,
                        SalaoService salaoService,
                        LimitadorTaxa limitador,
                        RelogioSalao relogio,
                        AppProperties props) {
        this.usuarioRepository = usuarioRepository;
        this.codigoRepository = codigoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.salaoService = salaoService;
        this.limitador = limitador;
        this.relogio = relogio;
        this.siteUrl = props.siteUrl() == null ? "" : props.siteUrl().replaceAll("/+$", "");
    }

    // ================================================================== conta logada

    @Transactional(readOnly = true)
    public ContaDtos.UsuarioResponse minhaConta(String email) {
        return ContaDtos.UsuarioResponse.de(buscarPorEmail(email));
    }

    /** Passo 1 da troca de senha: envia o código para o e-mail da própria conta. */
    @Transactional
    public String solicitarCodigoTroca(String email) {
        UsuarioAdmin u = buscarPorEmail(email);
        if (limitador.registrar("codigo-troca:" + u.getId(), Duration.ofHours(1)) > MAX_PEDIDOS_POR_HORA) {
            throw new RegraNegocioException("Você já pediu vários códigos. Aguarde um pouco e use o último que chegou.");
        }
        enviarCodigo(u, FinalidadeCodigo.TROCAR_SENHA, null);
        return "Enviamos um código de 6 números para " + mascarar(u.getEmail()) + ". Ele vale por 15 minutos.";
    }

    /** Passo 2 da troca de senha: confirma com o código recebido. */
    @Transactional(noRollbackFor = RegraNegocioException.class)   // grava a tentativa errada
    public void trocarSenha(String email, String codigo, String novaSenha, String tokenDispositivoAtual) {
        UsuarioAdmin u = buscarPorEmail(email);
        validarForca(novaSenha, u.getEmail());
        consumirCodigo(u, codigo, Set.of(FinalidadeCodigo.TROCAR_SENHA));
        u.definirSenhaHash(passwordEncoder.encode(novaSenha));
        // continua conectado só neste dispositivo; os outros precisarão confirmar de novo
        dispositivoRepository.removerTodosExceto(u.getId(), hashSha256(tokenDispositivoAtual == null ? "" : tokenDispositivoAtual));
        avisarSenhaAlterada(u);
    }

    // ================================================================== dispositivo novo

    /** Envia o código para confirmar um dispositivo novo (já com e-mail e senha corretos). */
    @Transactional
    public String solicitarCodigoDispositivo(String email, String nomeDispositivo) {
        UsuarioAdmin u = buscarPorEmail(email);
        if (limitador.registrar("codigo-dispositivo:" + u.getId(), Duration.ofHours(1)) > 5) {
            throw new RegraNegocioException("Você já pediu vários códigos. Aguarde um pouco e use o último que chegou.");
        }
        enviarCodigo(u, FinalidadeCodigo.NOVO_DISPOSITIVO, nomeDispositivo);
        return "Para sua segurança, enviamos um código de 6 números para " + mascarar(u.getEmail())
                + " para confirmar este dispositivo.";
    }

    /** Confere o código do dispositivo novo. @return a pessoa dona da conta */
    @Transactional(noRollbackFor = RegraNegocioException.class)
    public UsuarioAdmin confirmarCodigoDispositivo(String email, String codigo) {
        UsuarioAdmin u = buscarPorEmail(email);
        consumirCodigo(u, codigo, Set.of(FinalidadeCodigo.NOVO_DISPOSITIVO));
        return u;
    }

    // ================================================================== sem login

    /** Sempre responde a mesma coisa: não revela se o e-mail está cadastrado. */
    @Transactional
    public String esqueciSenha(String email) {
        String normalizado = email.trim().toLowerCase();
        if (limitador.registrar("esqueci:" + normalizado, Duration.ofHours(1)) > MAX_PEDIDOS_POR_HORA) {
            return RESPOSTA_ESQUECI;   // silencioso: evita usar o sistema para lotar a caixa de alguém
        }
        usuarioRepository.findByEmailIgnoreCase(normalizado)
                .filter(UsuarioAdmin::isAtivo)
                .ifPresent(u -> enviarCodigo(u, FinalidadeCodigo.REDEFINIR_SENHA, null));
        return RESPOSTA_ESQUECI;
    }

    /** Nova senha com o código do "esqueci" ou do convite (primeiro acesso). */
    @Transactional(noRollbackFor = RegraNegocioException.class)
    public void redefinirSenha(String email, String codigo, String novaSenha) {
        UsuarioAdmin u = usuarioRepository.findByEmailIgnoreCase(email.trim())
                .filter(UsuarioAdmin::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Código inválido ou expirado. Peça um novo."));
        validarForca(novaSenha, u.getEmail());
        consumirCodigo(u, codigo, Set.of(FinalidadeCodigo.REDEFINIR_SENHA, FinalidadeCodigo.CONVITE));
        u.definirSenhaHash(passwordEncoder.encode(novaSenha));
        dispositivoRepository.removerTodosExceto(u.getId(), "");   // todos precisarão confirmar de novo
        avisarSenhaAlterada(u);
    }

    // ================================================================== acessos da equipe (só DONO)

    @Transactional(readOnly = true)
    public List<ContaDtos.UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAllByOrderByPapelAscNomeAsc().stream()
                .map(ContaDtos.UsuarioResponse::de)
                .toList();
    }

    @Transactional
    public ContaDtos.UsuarioResponse convidar(ContaDtos.ConviteRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe um acesso com este e-mail.");
        }
        // senha aleatória e desconhecida: a pessoa cria a dela com o código do convite
        String senhaInutilizavel = passwordEncoder.encode(gerarCodigo() + gerarCodigo() + System.nanoTime());
        UsuarioAdmin u = usuarioRepository.save(new UsuarioAdmin(req.nome().trim(), email, senhaInutilizavel, req.papel()));
        enviarCodigo(u, FinalidadeCodigo.CONVITE, null);
        return ContaDtos.UsuarioResponse.de(u);
    }

    @Transactional
    public void reenviarConvite(Long id) {
        UsuarioAdmin u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acesso não encontrado."));
        if (!u.isAtivo()) {
            throw new RegraNegocioException("Reative o acesso antes de reenviar o convite.");
        }
        enviarCodigo(u, FinalidadeCodigo.CONVITE, null);
    }

    @Transactional
    public ContaDtos.UsuarioResponse alterarAtivo(Long id, boolean ativo, String emailQuemPediu) {
        UsuarioAdmin u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Acesso não encontrado."));
        if (!ativo && u.getEmail().equalsIgnoreCase(emailQuemPediu)) {
            throw new RegraNegocioException("Você não pode desativar o seu próprio acesso.");
        }
        if (!ativo && u.getPapel() == PapelUsuario.DONO && usuarioRepository.countByPapelAndAtivoTrue(PapelUsuario.DONO) <= 1) {
            throw new RegraNegocioException("Precisa existir pelo menos um dono ativo.");
        }
        u.setAtivo(ativo);
        if (!ativo) {
            dispositivoRepository.removerTodosExceto(u.getId(), "");
        }
        return ContaDtos.UsuarioResponse.de(u);
    }

    /** Todo dia, apaga códigos vencidos há mais de 7 dias. */
    @Scheduled(cron = "0 15 3 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void limparCodigosAntigos() {
        codigoRepository.apagarVencidosAntesDe(relogio.agoraOffset().minusDays(7));
        dispositivoRepository.removerVencidos(relogio.agoraOffset());
    }

    // ================================================================== privados

    private void enviarCodigo(UsuarioAdmin u, FinalidadeCodigo finalidade, String detalhe) {
        OffsetDateTime agora = relogio.agoraOffset();
        codigoRepository.invalidarPendentes(u.getId(), finalidade, agora);
        String codigo = gerarCodigo();
        codigoRepository.save(new CodigoVerificacao(u, finalidade, hash(codigo), agora.plus(finalidade.getValidade())));

        String salao = salaoService.informacoes().nome();
        String assunto = codigo + " é o seu código de acesso, " + salao;
        StringBuilder texto = new StringBuilder("Olá, ").append(primeiroNome(u.getNome())).append("!\n\n");
        if (finalidade == FinalidadeCodigo.NOVO_DISPOSITIVO) {
            texto.append("Alguém entrou no painel do ").append(salao)
                    .append(" com o seu e-mail e senha em um dispositivo novo");
            if (detalhe != null) {
                texto.append(" (").append(detalhe).append(")");
            }
            texto.append(".\n\nSe foi você, confirme o acesso com este código:\n\n        ").append(codigo)
                    .append("\n\nEle vale por 15 minutos.\n\n")
                    .append("SE NÃO FOI VOCÊ: não informe o código a ninguém e troque sua senha agora mesmo ")
                    .append("(\"Esqueci minha senha\" na tela de entrar). Sem o código, o acesso não é liberado.\n");
        } else if (finalidade == FinalidadeCodigo.CONVITE) {
            texto.append("Você foi convidado(a) para acessar o painel do ").append(salao).append(".\n\n")
                    .append("Para criar sua senha, acesse:\n")
                    .append(siteUrl).append("/admin/primeiro-acesso?email=").append(u.getEmail()).append("\n\n")
                    .append("e informe este código:\n\n        ").append(codigo).append("\n\n")
                    .append("O código vale por 48 horas.\n");
        } else {
            texto.append("Use este código para ").append(finalidade.getDescricao()).append(" no painel do ")
                    .append(salao).append(":\n\n        ").append(codigo).append("\n\n")
                    .append("Ele vale por 15 minutos. Se não foi você que pediu, ignore este e-mail: ")
                    .append("sua senha continua a mesma.\n");
        }
        texto.append("\nNunca compartilhe este código com ninguém.\n\n— ").append(salao);
        emailService.enviar(u.getEmail(), assunto, texto.toString());
    }

    /** Confere o código mais recente válido; conta tentativa errada. */
    private void consumirCodigo(UsuarioAdmin u, String codigo, Set<FinalidadeCodigo> finalidades) {
        OffsetDateTime agora = relogio.agoraOffset();
        CodigoVerificacao pendente = codigoRepository.findPendentes(u.getId(), finalidades).stream()
                .filter(c -> c.podeSerUsado(agora))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Código inválido ou expirado. Peça um novo."));

        boolean confere = MessageDigest.isEqual(
                hash(codigo.trim()).getBytes(StandardCharsets.UTF_8),
                pendente.getCodigoHash().getBytes(StandardCharsets.UTF_8));
        if (!confere) {
            pendente.registrarTentativaErrada();
            int restantes = CodigoVerificacao.MAX_TENTATIVAS - pendente.getTentativas();
            throw new RegraNegocioException(restantes > 0
                    ? "Código incorreto. Você ainda tem " + restantes + (restantes == 1 ? " tentativa." : " tentativas.")
                    : "Código incorreto. Este código foi bloqueado; peça um novo.");
        }
        pendente.marcarUsado(agora);
    }

    private void validarForca(String senha, String email) {
        if (senha == null || senha.length() < 8) {
            throw new RegraNegocioException("A senha precisa ter pelo menos 8 caracteres.");
        }
        if (senha.length() > 72) {
            throw new RegraNegocioException("A senha pode ter no máximo 72 caracteres.");
        }
        if (!senha.matches(".*[A-Za-zÀ-ÿ].*") || !senha.matches(".*\\d.*")) {
            throw new RegraNegocioException("Use letras e números na senha.");
        }
        if (senha.equalsIgnoreCase(email) || email.toLowerCase().startsWith(senha.toLowerCase())) {
            throw new RegraNegocioException("A senha não pode ser o seu e-mail.");
        }
    }

    private void avisarSenhaAlterada(UsuarioAdmin u) {
        String salao = salaoService.informacoes().nome();
        emailService.enviar(u.getEmail(), "Sua senha do painel foi alterada",
                "Olá, " + primeiroNome(u.getNome()) + "!\n\nA senha do seu acesso ao painel do " + salao
                        + " acabou de ser alterada.\n\nSe não foi você, avise o responsável pelo salão imediatamente.\n\n— "
                        + salao);
    }

    private UsuarioAdmin buscarPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
    }

    private static String gerarCodigo() {
        return String.format("%06d", ALEATORIO.nextInt(1_000_000));
    }

    static String hashSha256(String valor) {
        return hash(valor);
    }

    private static String hash(String valor) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String primeiroNome(String nome) {
        return nome == null || nome.isBlank() ? "" : nome.trim().split("\\s+")[0];
    }

    /** "ana.paula@gmail.com" → "an•••@gmail.com" */
    private static String mascarar(String email) {
        int arroba = email.indexOf('@');
        if (arroba <= 2) {
            return email;
        }
        return email.substring(0, 2) + "•••" + email.substring(arroba);
    }
}
