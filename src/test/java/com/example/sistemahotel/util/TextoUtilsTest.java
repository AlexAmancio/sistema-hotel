package com.example.sistemahotel.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TextoUtilsTest {

    @Test
    void capitalizaNombreEnMayusculas() {
        assertEquals("Alex Valencia Amancio", TextoUtils.capitalizarNombre("ALEX VALENCIA AMANCIO"));
    }

    @Test
    void capitalizaNombreEnMinusculas() {
        assertEquals("Alex Valencia Amancio", TextoUtils.capitalizarNombre("alex valencia amancio"));
    }

    @Test
    void capitalizaNombreMezclado() {
        assertEquals("Alex Valencia Amancio", TextoUtils.capitalizarNombre("aLEX ValenCia  Amancio"));
    }

    @Test
    void respetaAcentosYEnie() {
        assertEquals("José Muñoz", TextoUtils.capitalizarNombre("josé MUÑOZ"));
    }

    @Test
    void colapsaEspaciosDobles() {
        assertEquals("Ana Maria", TextoUtils.capitalizarNombre("  ana   maria  "));
    }

    @Test
    void manejaNuloYVacio() {
        assertNull(TextoUtils.capitalizarNombre(null));
        assertEquals("", TextoUtils.capitalizarNombre("   "));
    }
}
