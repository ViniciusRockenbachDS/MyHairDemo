package br.com.jofashionhair.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.com.jofashionhair.demo.ContextoDemo;
import br.com.jofashionhair.exception.RegraNegocioException;

/**
 * Versão da demonstração: as imagens ficam numa tabela do banco
 * (public.arquivos), porque o disco do plano gratuito do Render é apagado a
 * cada reinício. Imagens de visitantes são apagadas depois da visita.
 *
 * <p>Segurança: o tipo é conferido pelos primeiros bytes do arquivo, só
 * JPG/PNG/WEBP são aceitos e cada arquivo ganha um nome aleatório.</p>
 */
@Service
public class ArmazenamentoImagens {

    public static final String PREFIXO_URL = "/api/public/arquivos/";
    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
    private static final Pattern NOME_VALIDO = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png|webp)$");

    private final JdbcTemplate jdbc;

    public ArmazenamentoImagens(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.arquivos (
                    nome      VARCHAR(60) PRIMARY KEY,
                    dados     BYTEA       NOT NULL,
                    origem    VARCHAR(12) NOT NULL DEFAULT 'BASE',
                    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
                )""");
    }

    /** @return endereço público da imagem, ex.: /api/public/arquivos/1f2e....jpg */
    public String salvarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Escolha uma imagem.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("A imagem deve ter no máximo 5 MB.");
        }
        try {
            byte[] bytes = arquivo.getBytes();
            String extensao = detectarExtensao(bytes);
            if (extensao == null) {
                throw new RegraNegocioException("Envie uma imagem JPG, PNG ou WEBP.");
            }
            String nome = UUID.randomUUID() + "." + extensao;
            jdbc.update("INSERT INTO public.arquivos (nome, dados, origem) VALUES (?, ?, ?)",
                    nome, bytes, ContextoDemo.ehVisitante() ? "VISITANTE" : "BASE");
            return PREFIXO_URL + nome;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar a imagem", e);
        }
    }

    /** Conteúdo da imagem, se o nome for válido e ela existir. */
    public Optional<byte[]> carregar(String nome) {
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) {
            return Optional.empty();
        }
        List<byte[]> r = jdbc.query("SELECT dados FROM public.arquivos WHERE nome = ?",
                (rs, i) -> rs.getBytes(1), nome);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    private static String detectarExtensao(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
