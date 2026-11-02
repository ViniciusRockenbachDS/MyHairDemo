package br.com.jofashionhair.domain;

/**
 * Normaliza telefones para um formato ÚNICO: só dígitos, com DDI 55 e com o
 * 9º dígito nos celulares. Ex.: "(47) 99999-8888" -> "5547999998888".
 *
 * <p>Importante: o WhatsApp costuma identificar celulares brasileiros SEM o
 * 9º dígito (ex.: 554799998888). Sem esta regra, a mesma cliente viraria dois
 * cadastros (um pelo site, outro pelo WhatsApp).</p>
 */
public final class Telefone {

    private Telefone() {
    }

    public static String normalizar(String bruto) {
        if (bruto == null) {
            return null;
        }
        String digitos = bruto.replaceAll("\\D", "");
        // número brasileiro sem DDI (DDD + 8 ou 9 dígitos)
        if (digitos.length() == 10 || digitos.length() == 11) {
            digitos = "55" + digitos;
        }
        // 55 + DDD + 8 dígitos começando em 6-9 = celular sem o 9º dígito
        if (digitos.length() == 12 && digitos.startsWith("55")) {
            char primeiro = digitos.charAt(4);
            if (primeiro >= '6' && primeiro <= '9') {
                digitos = digitos.substring(0, 4) + "9" + digitos.substring(4);
            }
        }
        return digitos;
    }
}
