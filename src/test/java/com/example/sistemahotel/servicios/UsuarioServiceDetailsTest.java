package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.sistemahotel.modelos.Usuario;

class UsuarioServiceDetailsTest {

    @Test
    void unUsuarioAdminTieneRolAdminYStaff() {
        Usuario usuario = new Usuario();
        usuario.setUsername("elon");
        usuario.setContrasena("hash");
        usuario.setActivo(true);
        usuario.setEsAdmin(true);

        UsuarioServiceDetails details = new UsuarioServiceDetails(usuario);

        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")));
    }

    @Test
    void unUsuarioNoAdminSoloTieneRolStaff() {
        Usuario usuario = new Usuario();
        usuario.setUsername("gerardo");
        usuario.setContrasena("hash");
        usuario.setActivo(true);
        usuario.setEsAdmin(false);

        UsuarioServiceDetails details = new UsuarioServiceDetails(usuario);

        assertFalse(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")));
    }

    @Test
    void unUsuarioInactivoNoEstaHabilitado() {
        Usuario usuario = new Usuario();
        usuario.setUsername("elon");
        usuario.setContrasena("hash");
        usuario.setActivo(false);
        usuario.setEsAdmin(false);

        UsuarioServiceDetails details = new UsuarioServiceDetails(usuario);

        assertFalse(details.isEnabled());
    }
}
