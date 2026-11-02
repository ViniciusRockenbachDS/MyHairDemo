package br.com.jofashionhair.pagamento;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/** Transforma o "copia e cola" em imagem de QR Code (PNG em base64). */
public final class QrCodePng {

    private QrCodePng() {
    }

    public static String base64(String conteudo) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, 320, 320,
                    Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", saida);
            return Base64.getEncoder().encodeToString(saida.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Não foi possível gerar o QR Code do Pix", e);
        }
    }
}
