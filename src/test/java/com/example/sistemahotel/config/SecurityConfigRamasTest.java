package com.example.sistemahotel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Defecto D4: sin REMEMBER_ME_KEY (nula o en blanco) se genera una clave al
// azar; si está definida, se usa esa.
@Tag("unitaria")
class SecurityConfigRamasTest {

    private static String clave(String configurada) {
        SecurityConfig config = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(config, "claveRecuerdame", configurada);
        return config.claveRecuerdame();
    }

    @Test
    void usaLaClaveConfigurada() {
        assertEquals("clave-del-hotel", clave("clave-del-hotel"));
    }

    @Test
    void sinClaveOConClaveEnBlancoGeneraUnaAlAzar() {
        assertNotEquals("", clave(null));
        assertNotEquals("   ", clave("   "));
        assertNotEquals(clave(null), clave(null));
    }
}
