package br.com.jofashionhair.pagamento;

/**
 * @param pagamentoId  id do pagamento no Mercado Pago (null no modo manual)
 * @param copiaCola    código "Pix copia e cola"
 * @param qrCodeBase64 imagem PNG do QR Code
 */
public record PixGerado(String pagamentoId, String copiaCola, String qrCodeBase64) {
}
