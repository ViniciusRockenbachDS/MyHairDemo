package br.com.jofashionhair.demo;

/** Todas as vagas de demonstração estão em uso. */
public class DemoLotadaException extends RuntimeException {
    public DemoLotadaException() {
        super("A demonstração está cheia no momento. Tente de novo em alguns minutos.");
    }
}
