package br.com.jofashionhair.exception;

/** Recurso inexistente ou inativo (HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
