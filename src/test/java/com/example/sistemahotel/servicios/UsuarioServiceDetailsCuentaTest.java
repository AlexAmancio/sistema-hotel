package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.example.sistemahotel.modelos.Usuario;

// Datos de la cuenta que Spring Security lee al iniciar sesión (reglas RN21 y RN35).
@Tag("unitaria")
class UsuarioServiceDetailsCuentaTest {

    private UsuarioServiceDetails detalles;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setUsername("mquispe");
        usuario.setContrasena("$2a$10$hashDePrueba");
        usuario.setActivo(true);
        detalles = new UsuarioServiceDetails(usuario);
    }

    @Test
    void exponeElNombreDeUsuario() {
        assertEquals("mquispe", detalles.getUsername());
    }

    @Test
    void exponeLaContrasenaCifradaTalComoEstaGuardada() {
        assertEquals("$2a$10$hashDePrueba", detalles.getPassword());
    }

    @Test
    void laCuentaNoVenceNiSeBloqueaPorSiMisma() {
        assertTrue(detalles.isAccountNonExpired());
        assertTrue(detalles.isAccountNonLocked());
        assertTrue(detalles.isCredentialsNonExpired());
    }

    @Test
    void unUsuarioActivoEstaHabilitado() {
        assertTrue(detalles.isEnabled());
    }

    // El texto de depuración muestra solo el nombre de usuario, nunca la
    // contraseña cifrada (relacionado con el defecto D6).
    @Test
    void elTextoDeDepuracionMuestraSoloElNombreDeUsuario() {
        assertEquals("UsuarioServiceDetails[mquispe]", detalles.toString());
    }

    // La sesión única depende de equals: dos inicios de sesión del mismo
    // usuario son iguales; otro usuario u otro tipo de objeto, no.
    @Test
    void dosDetallesSonIgualesSoloSiSonDelMismoUsuario() {
        Usuario mismo = new Usuario();
        mismo.setUsername("mquispe");
        Usuario otro = new Usuario();
        otro.setUsername("aflores");

        assertTrue(detalles.equals(new UsuarioServiceDetails(mismo)));
        assertFalse(detalles.equals(new UsuarioServiceDetails(otro)));
        assertFalse(detalles.equals("mquispe"));
    }
}
