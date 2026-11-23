package com.example.sistemahotel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Usuario;

// Datos del usuario en sesión que se muestran en la barra lateral de todas las
// pantallas (RN21): nombre completo y rol, y nada si no hay sesión.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class GlobalModelAttributesTest {

    @Mock
    private IUsuarioService usuarioService;

    private GlobalModelAttributes atributos;
    private ExtendedModelMap model;

    @BeforeEach
    void setUp() {
        atributos = new GlobalModelAttributes();
        ReflectionTestUtils.setField(atributos, "usuarioService", usuarioService);
        model = new ExtendedModelMap();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void sesion(String usuario, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario, "x",
            java.util.Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList()));
    }

    @Test
    void sinSesionNoAgregaNada() {
        atributos.agregarUsuarioActual(model);

        assertTrue(model.isEmpty());
    }

    @Test
    void conUnaSesionNoAutenticadaNoAgregaNada() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("elon", "x"));

        atributos.agregarUsuarioActual(model);

        assertTrue(model.isEmpty());
    }

    @Test
    void conElUsuarioAnonimoNoAgregaNada() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("clave", "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        atributos.agregarUsuarioActual(model);

        assertTrue(model.isEmpty());
    }

    @Test
    void unAdministradorSeMuestraConSuNombreCompletoYSuRol() {
        sesion("elon", "ADMIN", "STAFF");
        Usuario elon = new Usuario();
        elon.setNombres("Elon");
        elon.setApellidos("Musk");
        when(usuarioService.consultarpornombreusuario("elon")).thenReturn(Optional.of(elon));

        atributos.agregarUsuarioActual(model);

        assertEquals("Elon Musk", model.get("usuarioActualNombre"));
        assertEquals("Administrador", model.get("usuarioActualRol"));
        assertEquals(true, model.get("usuarioActualEsAdmin"));
    }

    @Test
    void siElUsuarioYaNoExisteSeMuestraSuNombreDeUsuarioComoUsuarioNormal() {
        sesion("mquispe", "STAFF");
        when(usuarioService.consultarpornombreusuario("mquispe")).thenReturn(Optional.empty());

        atributos.agregarUsuarioActual(model);

        assertEquals("mquispe", model.get("usuarioActualNombre"));
        assertEquals("Usuario Normal", model.get("usuarioActualRol"));
        assertEquals(false, model.get("usuarioActualEsAdmin"));
    }
}
