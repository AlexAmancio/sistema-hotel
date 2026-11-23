package com.example.sistemahotel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Defecto D4: la clave de "Recuérdame" viene de la configuración; si no está
// definida se genera una al azar, nunca una clave fija y conocida.
@Tag("unitaria")
class SecurityConfigClaveTest {

    @Test
    void usaLaClaveConfigurada() {
        SecurityConfig config = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(config, "claveRecuerdame", "clave-del-servidor");

        assertEquals("clave-del-servidor", config.claveRecuerdame());
    }

    @Test
    void sinClaveConfiguradaGeneraUnaDistintaEnCadaArranque() {
        SecurityConfig config = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(config, "claveRecuerdame", "  ");

        String primera = config.claveRecuerdame();
        String segunda = config.claveRecuerdame();

        assertFalse(primera.isBlank());
        assertNotEquals(primera, segunda);
        assertNotEquals("estadia-remember-me-key", primera);
    }
}
