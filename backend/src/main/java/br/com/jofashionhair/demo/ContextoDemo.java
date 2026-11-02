package br.com.jofashionhair.demo;

/**
 * Em qual "cópia" do banco a requisição atual trabalha.
 *
 * <ul>
 *   <li>BASE: os dados originais (schema public), editados só pelo dono da demonstração.</li>
 *   <li>VISITANTE: a cópia particular do visitante (schema demo_N).</li>
 * </ul>
 * Fora do modo demonstração fica vazio e tudo usa o schema public.
 */
public final class ContextoDemo {

    public enum Modo { BASE, VISITANTE }

    public record Estado(String schema, Modo modo, String sessao) {
    }

    private static final ThreadLocal<Estado> ATUAL = new ThreadLocal<>();

    private ContextoDemo() {
    }

    public static void definir(String schema, Modo modo, String sessao) {
        ATUAL.set(new Estado(schema, modo, sessao));
    }

    public static String schema() {
        Estado e = ATUAL.get();
        return e == null ? "public" : e.schema();
    }

    public static boolean ehVisitante() {
        Estado e = ATUAL.get();
        return e != null && e.modo() == Modo.VISITANTE;
    }

    public static String sessao() {
        Estado e = ATUAL.get();
        return e == null ? null : e.sessao();
    }

    public static Estado capturar() {
        return ATUAL.get();
    }

    public static void restaurar(Estado estado) {
        if (estado == null) {
            ATUAL.remove();
        } else {
            ATUAL.set(estado);
        }
    }

    public static void limpar() {
        ATUAL.remove();
    }
}
