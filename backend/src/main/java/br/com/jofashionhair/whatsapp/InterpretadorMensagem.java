package br.com.jofashionhair.whatsapp;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.jofashionhair.domain.Profissional;
import br.com.jofashionhair.domain.Servico;

/**
 * Entende mensagens em português informal: datas ("sexta", "amanhã", "dia 16",
 * "16/10"), horários ("17h", "17:30", "às 5 da tarde"), números de menu,
 * nomes de serviços/profissionais e intenções (cancelar, atendente, menu...).
 *
 * <p>Classe sem estado e sem banco: fácil de testar (ver InterpretadorMensagemTest).</p>
 */
@Component
public class InterpretadorMensagem {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");
    private static final Pattern DATA_BARRA = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}|\\d{2}))?\\b");
    private static final Pattern DIA_DO_MES = Pattern.compile("\\bdia\\s+(\\d{1,2})\\b");
    private static final Pattern DIA_SEMANA = Pattern.compile("\\b(segunda|terca|quarta|quinta|sexta|sabado|domingo)\\b");
    private static final Pattern HORA_PERIODO = Pattern.compile("\\b(\\d{1,2})\\s*(?:h\\s*)?da\\s+(manha|tarde|noite)\\b");
    private static final Pattern HORA_SEPARADOR = Pattern.compile("\\b(\\d{1,2})\\s*(?:h(?:rs?|s|oras?)?|:)\\s*(\\d{2})?(?!\\d)");
    private static final Pattern HORA_APOS_AS = Pattern.compile("\\b(?:as|das|pelas)\\s+(\\d{1,2})\\b(?!\\s*/)");
    private static final Pattern OPCAO = Pattern.compile("^(?:opcao|numero|op|n)?\\s*(\\d{1,2})\\s*[.)\\-]?$");

    private static final Map<String, DayOfWeek> DIAS = Map.of(
            "segunda", DayOfWeek.MONDAY, "terca", DayOfWeek.TUESDAY, "quarta", DayOfWeek.WEDNESDAY,
            "quinta", DayOfWeek.THURSDAY, "sexta", DayOfWeek.FRIDAY, "sabado", DayOfWeek.SATURDAY,
            "domingo", DayOfWeek.SUNDAY);

    /** Palavras que não identificam serviço (já no singular e sem acento). */
    private static final Set<String> STOPWORDS = Set.of(
            "quero", "queria", "gostaria", "marcar", "agendar", "agendamento", "horario", "para", "fazer",
            "favor", "segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo", "feira",
            "amanha", "hoje", "tarde", "manha", "noite", "hora", "entao", "tudo", "voce", "pode", "seria",
            "mais", "menos", "ainda", "aqui", "isso", "esse", "essa", "depois", "semana", "proxima",
            "proximo", "obrigado", "obrigada", "muito", "preciso", "tenho", "consigo", "saber", "valor",
            "quanto", "custa", "oi", "ola", "bom", "boa", "dia", "salao", "fashion", "hair");

    /** Data entendida; {@code duvida} vem preenchida quando dia da semana e data não batem. */
    public record DataInterpretada(LocalDate data, String duvida) {
        static DataInterpretada nenhuma() {
            return new DataInterpretada(null, null);
        }
    }

    // ===================================================================== texto

    /** minúsculas, sem acentos, espaços simples. Emojis de número (1️⃣) viram "1". */
    public String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD);
        s = MARCAS.matcher(s).replaceAll("");
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    // ===================================================================== data e hora

    public DataInterpretada extrairData(String n, LocalDate hoje) {
        if (n.contains("depois de amanha")) {
            return new DataInterpretada(hoje.plusDays(2), null);
        }
        if (n.contains("amanha")) {
            return new DataInterpretada(hoje.plusDays(1), null);
        }
        if (n.matches(".*\\bhoje\\b.*")) {
            return new DataInterpretada(hoje, null);
        }

        LocalDate explicita = null;
        Matcher barra = DATA_BARRA.matcher(n);
        if (barra.find()) {
            explicita = montarData(barra.group(1), barra.group(2), barra.group(3), hoje);
        } else {
            Matcher dia = DIA_DO_MES.matcher(n);
            if (dia.find()) {
                explicita = proximoDiaDoMes(Integer.parseInt(dia.group(1)), hoje);
            }
        }

        DayOfWeek diaSemana = null;
        Matcher semana = DIA_SEMANA.matcher(n);
        if (semana.find()) {
            diaSemana = DIAS.get(semana.group(1));
        }

        if (explicita != null && diaSemana != null && explicita.getDayOfWeek() != diaSemana) {
            LocalDate pelaSemana = hoje.with(TemporalAdjusters.nextOrSame(diaSemana));
            return new DataInterpretada(null, "Fiquei em dúvida 🤔: o dia " + DIA_MES.format(explicita)
                    + " cai " + (masculino(explicita.getDayOfWeek()) ? "num " : "numa ")
                    + nomeDia(explicita.getDayOfWeek()) + ", e "
                    + (masculino(diaSemana) ? "o próximo " : "a próxima ") + nomeDia(diaSemana)
                    + " é dia " + DIA_MES.format(pelaSemana) + ". Qual data você prefere? (ex.: *"
                    + DIA_MES.format(pelaSemana) + "*)");
        }
        if (explicita != null) {
            return new DataInterpretada(explicita, null);
        }
        if (diaSemana != null) {
            return new DataInterpretada(hoje.with(TemporalAdjusters.nextOrSame(diaSemana)), null);
        }
        return DataInterpretada.nenhuma();
    }

    public Optional<LocalTime> extrairHora(String n) {
        if (n.contains("meio dia") || n.contains("meio-dia")) {
            return Optional.of(LocalTime.NOON);
        }
        Matcher periodo = HORA_PERIODO.matcher(n);
        if (periodo.find()) {
            int h = Integer.parseInt(periodo.group(1));
            if (!"manha".equals(periodo.group(2)) && h < 12) {
                h += 12;
            }
            return montarHora(h, 0);
        }
        Matcher sep = HORA_SEPARADOR.matcher(n);
        if (sep.find()) {
            int h = Integer.parseInt(sep.group(1));
            int min = sep.group(2) == null ? 0 : Integer.parseInt(sep.group(2));
            return montarHora(ajustarPeriodo(h, n), min);
        }
        Matcher as = HORA_APOS_AS.matcher(n);
        if (as.find()) {
            return montarHora(ajustarPeriodo(Integer.parseInt(as.group(1)), n), 0);
        }
        return Optional.empty();
    }

    // ===================================================================== menus e intenções

    /** "2", "2.", "opção 2", "2️⃣" → 2. Qualquer outra coisa → null. */
    public Integer extrairOpcao(String n) {
        Matcher m = OPCAO.matcher(n);
        return m.matches() ? Integer.valueOf(m.group(1)) : null;
    }

    /** "tabela", "valores", "quanto custa"... → envia a tabela de preços. */
    public boolean pedeTabela(String n) {
        return contemAlgum(n, "tabela", "valores", "precos", "preco", "cardapio", "quanto custa", "quanto ta",
                "quanto esta", "quanto fica", "quanto sai", "quanto e ");
    }

    /** Parece uma pergunta ("?" ou começa com "vocês", "aceitam", "tem", "qual"...). */
    public boolean ehPergunta(String n) {
        if (n.contains("?")) {
            return true;
        }
        return soPalavras(n).matches("^(oi |ola |bom dia |boa tarde |boa noite )?(voces|vcs|vc|voce|aceita|aceitam"
                + "|tem|teria|qual|quais|como|onde|quando|pode|posso|faz|fazem|e possivel|funciona|precisa|preciso"
                + "|quanto tempo|ate que horas|abre|abrem|atende|atendem|existe|da pra|da para)\\b.*");
    }

    /** "paguei", "fiz o pix", "segue o comprovante"... */
    public boolean avisaPagamento(String n) {
        return contemAlgum(n, "paguei", "pago", "fiz o pix", "pix feito", "comprovante", "transferi", "ja pagamos");
    }

    /** "SAIR", "parar", "não quero mais receber" → não receber mais promoções. */
    public boolean pedeSairPromocoes(String n) {
        return soPalavras(n).matches("^(sair|parar|pare|stop|descadastrar|cancelar promocoes"
                + "|nao quero (mais )?receber( (promocoes|promocao|mensagens))?)$");
    }

    /** Mensagem que é só um cumprimento ("oi", "boa tarde, tudo bem?"). */
    public boolean ehSaudacao(String n) {
        String s = soPalavras(n);
        return s.matches("^(oi+|ola+|opa|eai|e ai|hey|hello|bom dia|boa tarde|boa noite)"
                + "( (tudo bem|td bem|tudo bom|como vai|como vai voce|tudo certo))?$");
    }

    public boolean ehSemPreferencia(String n) {
        return contemAlgum(n, "sem preferencia", "tanto faz", "qualquer", "nenhuma", "indiferente",
                "nao tenho preferencia", "quem estiver livre", "quem tiver horario");
    }

    public boolean pedeAtendente(String n) {
        return contemAlgum(n, "atendente", "humano", "falar com alguem", "falar com uma pessoa",
                "pessoa de verdade", "falar com voces");
    }

    public boolean pedeCancelamento(String n) {
        return contemAlgum(n, "cancelar", "desmarcar", "cancelamento", "cancela ") || n.equals("cancela");
    }

    public boolean ehMenu(String n) {
        return Set.of("menu", "inicio", "recomecar", "reiniciar", "comecar de novo", "voltar ao inicio",
                "voltar").contains(n);
    }

    public boolean ehSim(String n) {
        return n.matches("^(sim|s|confirmo|confirmar|confirmado|confirmada|ok|okay|pode ser|claro|isso"
                + "|beleza|blz|certo|com certeza|vou sim|estarei ai|estarei la|vou)\\b.*");
    }

    public boolean ehNao(String n) {
        return n.matches("^(nao|n|negativo|nao vou|nao posso|nenhum)\\b.*") || pedeCancelamento(n);
    }

    public boolean ehAgradecimento(String n) {
        return n.matches("^(obrigad[oa]|obg|brigad[oa]|valeu|vlw|ok|okay|beleza|blz|show|perfeito|otimo|top"
                + "|certo|combinado|tchau|ate mais|ate logo)\\b.*")
                || n.contains("👍") || n.contains("❤") || n.contains("🙏") || n.contains("🥰");
    }

    // ===================================================================== catálogo

    /**
     * Serviços citados na mensagem.
     * 1º: nome completo citado (se mais de um, fica o mais longo: "unha em gel" > "unha").
     * 2º: palavra significativa em comum ("unhas" → "Unha em Gel").
     * Mais de um resultado = o robô pergunta qual.
     */
    public List<Servico> encontrarServicos(String n, List<Servico> catalogo) {
        List<Servico> porNome = catalogo.stream()
                .filter(s -> mencionaNomeCompleto(n, s.getNome()))
                .toList();
        if (!porNome.isEmpty()) {
            int maior = porNome.stream().mapToInt(s -> s.getNome().length()).max().orElse(0);
            return porNome.stream().filter(s -> s.getNome().length() == maior).toList();
        }
        Set<String> palavras = palavrasSignificativas(n);
        if (palavras.isEmpty()) {
            return List.of();
        }
        return catalogo.stream()
                .filter(s -> tokens(normalizar(s.getNome())).stream().map(this::singular).anyMatch(palavras::contains))
                .toList();
    }

    public boolean mencionaNomeCompleto(String n, String nome) {
        String alvo = soPalavras(normalizar(nome));
        return !alvo.isEmpty() && (" " + soPalavras(n) + " ").contains(" " + alvo + " ");
    }

    /** Profissional citada pelo nome completo ou pelo primeiro nome (só se for única). */
    public Optional<Profissional> encontrarProfissional(String n, List<Profissional> lista) {
        List<Profissional> porNomeCompleto = lista.stream()
                .filter(p -> mencionaNomeCompleto(n, p.getNome()))
                .toList();
        if (porNomeCompleto.size() == 1) {
            return Optional.of(porNomeCompleto.get(0));
        }
        Set<String> palavras = new HashSet<>(tokens(n));
        List<Profissional> porPrimeiroNome = lista.stream()
                .filter(p -> {
                    List<String> t = tokens(normalizar(p.getNome()));
                    return !t.isEmpty() && t.get(0).length() >= 3 && palavras.contains(t.get(0));
                })
                .toList();
        return porPrimeiroNome.size() == 1 ? Optional.of(porPrimeiroNome.get(0)) : Optional.empty();
    }

    // ===================================================================== auxiliares

    private LocalDate montarData(String dia, String mes, String ano, LocalDate hoje) {
        try {
            int a = ano == null ? hoje.getYear() : (ano.length() == 2 ? 2000 + Integer.parseInt(ano) : Integer.parseInt(ano));
            LocalDate data = LocalDate.of(a, Integer.parseInt(mes), Integer.parseInt(dia));
            if (ano == null && data.isBefore(hoje)) {
                data = data.plusYears(1);
            }
            return data;
        } catch (DateTimeException e) {
            return null;
        }
    }

    /** "dia 5" = o próximo dia 5 (este mês ou o seguinte). */
    private LocalDate proximoDiaDoMes(int dia, LocalDate hoje) {
        for (int i = 0; i < 3; i++) {
            LocalDate mes = hoje.plusMonths(i);
            try {
                LocalDate data = LocalDate.of(mes.getYear(), mes.getMonth(), dia);
                if (!data.isBefore(hoje)) {
                    return data;
                }
            } catch (DateTimeException e) {
                // mês sem esse dia (ex.: 31/11): tenta o próximo
            }
        }
        return null;
    }

    private int ajustarPeriodo(int h, String n) {
        if (h >= 1 && h <= 11 && (n.contains("tarde") || n.contains("noite"))) {
            return h + 12;
        }
        return h;
    }

    private Optional<LocalTime> montarHora(int h, int min) {
        if (h < 0 || h > 23 || min < 0 || min > 59) {
            return Optional.empty();
        }
        return Optional.of(LocalTime.of(h, min));
    }

    private boolean masculino(DayOfWeek dia) {
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    private String nomeDia(DayOfWeek dia) {
        return dia.getDisplayName(TextStyle.FULL, PT_BR);
    }

    private boolean contemAlgum(String n, String... termos) {
        for (String t : termos) {
            if (n.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private String soPalavras(String s) {
        return s.replaceAll("[^a-z0-9]+", " ").trim();
    }

    private List<String> tokens(String s) {
        String limpo = soPalavras(s);
        return limpo.isEmpty() ? List.of() : Arrays.asList(limpo.split(" "));
    }

    private String singular(String t) {
        return t.length() > 4 && t.endsWith("s") ? t.substring(0, t.length() - 1) : t;
    }

    private Set<String> palavrasSignificativas(String n) {
        return tokens(n).stream()
                .filter(t -> t.length() >= 4)
                .map(this::singular)
                .filter(t -> !STOPWORDS.contains(t))
                .collect(Collectors.toSet());
    }
}
