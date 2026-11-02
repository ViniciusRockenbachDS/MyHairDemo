package br.com.jofashionhair.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import br.com.jofashionhair.service.DisponibilidadeService;
import br.com.jofashionhair.service.SugestaoHorario;

/**
 * Converte exceções em respostas padronizadas (RFC 7807 / ProblemDetail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int MAX_SUGESTOES = 5;

    private final DisponibilidadeService disponibilidade;

    public GlobalExceptionHandler(DisponibilidadeService disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    /**
     * 409 + horários alternativos. As sugestões são calculadas aqui, numa
     * transação NOVA, porque a transação da reserva já foi desfeita.
     */
    @ExceptionHandler(HorarioIndisponivelException.class)
    public ProblemDetail horarioIndisponivel(HorarioIndisponivelException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Horário indisponível");
        pd.setProperty("motivo", ex.getMotivo().name());

        List<SugestaoHorario> sugestoes = List.of();
        try {
            sugestoes = disponibilidade.sugerirAlternativas(
                    ex.getServicoId(), ex.getProfissionalId(), ex.getInicio(), MAX_SUGESTOES);
        } catch (RuntimeException e) {
            log.warn("Falha ao calcular sugestões de horário", e);
        }
        pd.setProperty("sugestoes", sugestoes);
        return pd;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Não encontrado");
        return pd;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Operação não permitida");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos.");
        pd.setTitle("Requisição inválida");
        pd.setProperty("campos", campos);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpoInvalido(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido. Confira datas (AAAA-MM-DD) e horários (HH:mm).");
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getName() + "'.");
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail parametroAusente(MissingServletRequestParameterException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'.");
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail arquivoGrande(MaxUploadSizeExceededException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "A imagem é grande demais. Envie uma imagem de até 5 MB.");
        pd.setTitle("Arquivo grande demais");
        return pd;
    }
}
