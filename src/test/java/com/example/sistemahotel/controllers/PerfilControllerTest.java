package com.example.sistemahotel.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.sistemahotel.modelos.Usuario;

// Mi cuenta (HU05 y HU06) y regla RN20: para cambiar la contraseña se exige la
// actual y la confirmación de la nueva.
@WithMockUser(username = "elon", roles = "ADMIN")
class PerfilControllerTest extends ControladorIntegracionBase {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario elon;

    @BeforeEach
    void setUp() {
        elon = new Usuario();
        elon.setId(1);
        elon.setUsername("elon");
        elon.setNombres("Elon");
        elon.setApellidos("Musk");
        elon.setContrasena(passwordEncoder.encode("ClaveActual1"));
        when(usuarioService.consultarpornombreusuario("elon")).thenReturn(Optional.of(elon));
    }

    @Test
    void elPerfilMuestraLosDatosDelUsuarioEnSesion() throws Exception {
        mockMvc.perform(get("/perfil/"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("usuario", elon));
    }

    @Test
    void conLaContrasenaActualIncorrectaNoSeCambia() throws Exception {
        mockMvc.perform(post("/perfil/cambiar-contrasena/").with(csrf()).param("actual", "otra").param("nueva", "Nueva2026").param("confirmar", "Nueva2026"))
            .andExpect(redirectedUrl("/perfil/"))
            .andExpect(flash().attribute("error", "La contraseña actual no es correcta."));
        verify(usuarioService, never()).guardar(any());
    }

    @Test
    void unaContrasenaNuevaVaciaSeRechaza() throws Exception {
        mockMvc.perform(post("/perfil/cambiar-contrasena/").with(csrf()).param("actual", "ClaveActual1").param("nueva", " ").param("confirmar", " "))
            .andExpect(redirectedUrl("/perfil/"))
            .andExpect(flash().attribute("error", "La nueva contraseña no puede estar vacía."));
    }

    @Test
    void siLaConfirmacionNoCoincideSeRechaza() throws Exception {
        mockMvc.perform(post("/perfil/cambiar-contrasena/").with(csrf()).param("actual", "ClaveActual1").param("nueva", "Nueva2026").param("confirmar", "Otra2026"))
            .andExpect(redirectedUrl("/perfil/"))
            .andExpect(flash().attribute("error", "Las contraseñas nuevas no coinciden."));
    }

    @Test
    void conDatosCorrectosSeGuardaLaNuevaContrasenaCifrada() throws Exception {
        mockMvc.perform(post("/perfil/cambiar-contrasena/").with(csrf()).param("actual", "ClaveActual1").param("nueva", "Nueva2026").param("confirmar", "Nueva2026"))
            .andExpect(redirectedUrl("/perfil/"))
            .andExpect(flash().attribute("exito", "Contraseña actualizada correctamente."));
        verify(usuarioService).guardar(elon);
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Nueva2026", elon.getContrasena()));
    }
}
