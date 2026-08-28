package com.example.sistemahotel.util;

public class TextoUtils {
    private TextoUtils() {}

    // Normaliza nombres, apellidos, ciudades, etc. a "Cada Palabra Con Mayúscula
    // Inicial", sin importar cómo lo haya escrito quien lo tipeó (todo mayúsculas,
    // todo minúsculas, mezclado). No usar esto con correos electrónicos.
    public static String capitalizarNombre(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            return limpio;
        }
        StringBuilder resultado = new StringBuilder(limpio.length());
        boolean inicioDePalabra = true;
        for (char c : limpio.toCharArray()) {
            if (Character.isWhitespace(c)) {
                resultado.append(c);
                inicioDePalabra = true;
            } else if (inicioDePalabra) {
                resultado.append(Character.toUpperCase(c));
                inicioDePalabra = false;
            } else {
                resultado.append(Character.toLowerCase(c));
            }
        }
        return resultado.toString();
    }
}
