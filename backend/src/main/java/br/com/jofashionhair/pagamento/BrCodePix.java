package br.com.jofashionhair.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Gera o "Pix copia e cola" (BR Code estático) a partir da chave Pix do salão,
 * no padrão EMV do Banco Central, com valor e identificador. Não depende de
 * nenhuma empresa de pagamento.
 */
public final class BrCodePix {

    private BrCodePix() {
    }

    /**
     * @param chave         chave Pix (CPF/CNPJ, e-mail, telefone +55..., ou aleatória)
     * @param nomeRecebedor até 25 letras
     * @param cidade        até 15 letras
     * @param valor         valor a pagar
     * @param identificador até 25 letras/números (aparece no extrato)
     */
    public static String gerar(String chave, String nomeRecebedor, String cidade, BigDecimal valor, String identificador) {
        String contaPix = campo("00", "br.gov.bcb.pix") + campo("01", chave.trim());
        String txid = identificador == null || identificador.isBlank()
                ? "***" : identificador.replaceAll("[^A-Za-z0-9]", "");
        if (txid.length() > 25) txid = txid.substring(0, 25);

        String payload = campo("00", "01")
                + campo("26", contaPix)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", limpar(nomeRecebedor, 25))
                + campo("60", limpar(cidade, 15))
                + campo("62", campo("05", txid))
                + "6304";
        return payload + crc16(payload);
    }

    private static String campo(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** Sem acentos, maiúsculas, só letras/números/espaço, no tamanho máximo. */
    private static String limpar(String texto, int max) {
        String s = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", "")
                .trim()
                .toUpperCase();
        if (s.isEmpty()) s = "SALAO";
        return s.length() > max ? s.substring(0, max).trim() : s;
    }

    /** CRC16-CCITT (polinômio 0x1021, início 0xFFFF), exigido pelo padrão. */
    static String crc16(String payload) {
        int crc = 0xFFFF;
        for (byte b : payload.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
