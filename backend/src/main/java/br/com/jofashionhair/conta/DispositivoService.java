package br.com.jofashionhair.conta;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jofashionhair.config.AppProperties;
import br.com.jofashionhair.exception.RecursoNaoEncontradoException;
import br.com.jofashionhair.exception.RegraNegocioException;
import br.com.jofashionhair.service.RelogioSalao;

/**
 * Confirmação de dispositivo novo.
 *
 * <p>Depois de e-mail e senha corretos, o painel só libera o acesso se o
 * navegador apresentar o cookie de um dispositivo já confirmado. Se não, a
 * pessoa recebe um código por e-mail; ao confirmar, o navegador ganha um token
 * aleatório (cookie HttpOnly, inacessível ao JavaScript) válido por 90 dias.</p>
 */
@Service
public class DispositivoService {

    public static final String COOKIE = "jfh_dispositivo";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public record DispositivoResponse(Long id, String nome, LocalDateTime confirmadoEm,
                                      LocalDateTime ultimoUso, boolean atual) {
    }

    private final DispositivoConfiavelRepository repository;
    private final ContaService contaService;
    private final UsuarioAdminRepository usuarioRepository;
    private final RelogioSalao relogio;
    private final int diasValidade;

    public DispositivoService(DispositivoConfiavelRepository repository, ContaService contaService,
                              UsuarioAdminRepository usuarioRepository, RelogioSalao relogio, AppProperties props) {
        this.repository = repository;
        this.contaService = contaService;
        this.usuarioRepository = usuarioRepository;
        this.relogio = relogio;
        int dias = props.seguranca() == null ? 0 : props.seguranca().diasDispositivoConfiavel();
        this.diasValidade = dias > 0 ? dias : 90;
    }

    /** true se o token é de um dispositivo confirmado, ainda válido, desta pessoa. */
    @Transactional
    public boolean confiavel(String email, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Optional<DispositivoConfiavel> d = repository.findByTokenHash(ContaService.hashSha256(token));
        if (d.isEmpty() || !d.get().getUsuario().getEmail().equalsIgnoreCase(email)) {
            return false;
        }
        OffsetDateTime agora = relogio.agoraOffset();
        if (d.get().getExpiraEm().isBefore(agora)) {
            return false;
        }
        if (d.get().getUltimoUsoEm().isBefore(agora.minusHours(1))) {
            d.get().registrarUso(agora);   // grava no máximo 1x por hora
        }
        return true;
    }

    public String enviarCodigo(String email, String userAgent) {
        return contaService.solicitarCodigoDispositivo(email, NomeDispositivo.de(userAgent));
    }

    /** Confere o código e registra o dispositivo. @return token para o cookie */
    @Transactional(noRollbackFor = RegraNegocioException.class)
    public String confirmar(String email, String codigo, String userAgent, String ip) {
        UsuarioAdmin u = contaService.confirmarCodigoDispositivo(email, codigo);
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        OffsetDateTime agora = relogio.agoraOffset();
        repository.save(new DispositivoConfiavel(u, ContaService.hashSha256(token), NomeDispositivo.de(userAgent),
                ip, agora, agora.plusDays(diasValidade)));
        return token;
    }

    public int diasValidade() {
        return diasValidade;
    }

    @Transactional(readOnly = true)
    public List<DispositivoResponse> listar(String email, String tokenAtual) {
        UsuarioAdmin u = usuario(email);
        String hashAtual = tokenAtual == null ? "" : ContaService.hashSha256(tokenAtual);
        return repository.findByUsuarioIdOrderByUltimoUsoEmDesc(u.getId()).stream()
                .map(d -> new DispositivoResponse(d.getId(), d.getNome(),
                        relogio.paraLocal(d.getCriadoEm()), relogio.paraLocal(d.getUltimoUsoEm()),
                        d.getTokenHash().equals(hashAtual)))
                .toList();
    }

    @Transactional
    public void remover(String email, Long id) {
        UsuarioAdmin u = usuario(email);
        DispositivoConfiavel d = repository.findById(id)
                .filter(x -> x.getUsuario().getId().equals(u.getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Dispositivo não encontrado."));
        repository.delete(d);
    }

    @Transactional
    public int sairDosOutros(String email, String tokenAtual) {
        UsuarioAdmin u = usuario(email);
        return repository.removerTodosExceto(u.getId(),
                ContaService.hashSha256(tokenAtual == null ? "" : tokenAtual));
    }

    private UsuarioAdmin usuario(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
    }
}
