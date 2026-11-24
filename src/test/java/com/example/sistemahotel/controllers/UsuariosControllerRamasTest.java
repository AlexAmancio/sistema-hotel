package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Contraseña nula (no solo vacía), sesión sin autenticación y reglas del
// único administrador cuando hay más de uno o cuando el que se desactiva
// ya estaba inactivo.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuariosControllerRamasTest {

    @Mock private IUsuarioService service;
    @Mock private IRegistroService registroService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UsuariosController controller;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario usuario(int id, boolean admin, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("u" + id);
        u.setNombres("ana");
        u.setApellidos("rojas");
        u.setEsAdmin(admin);
        u.setActivo(activo);
        return u;
    }

    @Test
    void sinSesionLaListaNoMuestraAccionesDeAdministrador() {
        SecurityContextHolder.clearContext();
        ExtendedModelMap model = new ExtendedModelMap();

        controller.listar(model);

        assertEquals(false, model.get("esAdmin"));
    }

    @Test
    void unUsuarioNuevoSinContrasenaNoSeGuarda() {
        Usuario nuevo = usuario(0, false, true);
        nuevo.setContrasena(null);

        String vista = controller.guardar(nuevo, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertEquals("redirect:/usuarios/nuevo/", vista);
        verify(service, never()).guardar(any());
    }

    @Test
    void editarSinContrasenaConservaElHashGuardado() {
        Usuario existente = usuario(4, false, true);
        existente.setContrasena("hash-guardado");
        when(service.consultarpornombreusuario("u4")).thenReturn(Optional.empty());
        when(service.consultarId(4)).thenReturn(Optional.of(existente));
        Usuario editado = usuario(4, false, true);
        editado.setContrasena(null);

        controller.guardar(editado, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertEquals("hash-guardado", editado.getContrasena());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void seEliminaUnAdministradorSiQuedaOtroActivoYNoTieneRegistros() {
        Usuario admin1 = usuario(1, true, true);
        Usuario admin2 = usuario(2, true, true);
        when(service.consultarId(1)).thenReturn(Optional.of(admin1));
        when(service.listar()).thenReturn(List.of(admin1, admin2));
        when(registroService.listar()).thenReturn(List.of(new Registro()));

        String vista = controller.eliminar(1, new RedirectAttributesModelMap());

        assertEquals("redirect:/usuarios/", vista);
        verify(service).eliminar(1);
    }

    @Test
    void seDesactivaUnAdministradorInactivoAunqueSoloQuedeOtroActivo() {
        Usuario adminInactivo = usuario(1, true, false);
        Usuario adminActivo = usuario(2, true, true);
        when(service.consultarId(1)).thenReturn(Optional.of(adminInactivo));
        when(service.listar()).thenReturn(List.of(adminInactivo, adminActivo));

        controller.desactivar(1, new RedirectAttributesModelMap());

        verify(service).guardar(adminInactivo);
    }
}
