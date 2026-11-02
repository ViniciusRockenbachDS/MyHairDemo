package br.com.jofashionhair.conta;

/** "Mozilla/5.0 (Windows NT 10.0...) Chrome/..." → "Chrome no Windows" */
final class NomeDispositivo {

    private NomeDispositivo() {
    }

    static String de(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Dispositivo desconhecido";
        }
        String ua = userAgent;
        String navegador = ua.contains("Edg/") ? "Edge"
                : ua.contains("OPR/") || ua.contains("Opera") ? "Opera"
                : ua.contains("SamsungBrowser") ? "Samsung Internet"
                : ua.contains("Firefox/") ? "Firefox"
                : ua.contains("Chrome/") || ua.contains("CriOS/") ? "Chrome"
                : ua.contains("Safari/") ? "Safari"
                : "Navegador";
        String sistema = ua.contains("iPhone") ? "iPhone"
                : ua.contains("iPad") ? "iPad"
                : ua.contains("Android") ? "Android"
                : ua.contains("Windows") ? "Windows"
                : ua.contains("Mac OS X") || ua.contains("Macintosh") ? "Mac"
                : ua.contains("Linux") ? "Linux"
                : "sistema desconhecido";
        return navegador + " no " + sistema;
    }
}
