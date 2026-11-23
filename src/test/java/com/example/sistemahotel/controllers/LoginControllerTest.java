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

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;

// Recuperación de contraseña (HU03 y HU04) y reglas RN19 y RN36.
@WithAnonymousUser
class LoginControllerTest extends ControladorIntegracionBase {

    @Test
    void pedirLaRecuperacionMuestraElMismoAvisoExistaONoElCorreo() throws Exception {
        mockMvc.perform(post("/login/olvide-contrasena").with(csrf()).param("correo", "no.existe@ejemplo.test"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("enviado", true));
        verify(passwordResetService).solicitarReseteo("no.existe@ejemplo.test");
    }

    @Test
    void unEnlaceInvalidoOVencidoSeMuestraComoInvalido() throws Exception {
        when(passwordResetService.tokenValido("token-viejo")).thenReturn(false);

        mockMvc.perform(get("/login/restablecer").param("token", "token-viejo"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("estado", "invalido"));
    }

    @Test
    void unEnlaceValidoMuestraElFormulario() throws Exception {
        when(passwordResetService.tokenValido("token-bueno")).thenReturn(true);

        mockMvc.perform(get("/login/restablecer").param("token", "token-bueno"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("estado", "formulario"));
    }

    @Test
    void restablecerConUnEnlaceValidoMuestraExito() throws Exception {
        when(passwordResetService.restablecerContrasena("token-bueno", "Nueva2026")).thenReturn(true);

        mockMvc.perform(post("/login/restablecer").with(csrf()).param("token", "token-bueno").param("nuevaContrasena", "Nueva2026"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("estado", "exito"));
    }

    @Test
    void restablecerConUnEnlaceUsadoNoCambiaLaContrasena() throws Exception {
        when(passwordResetService.restablecerContrasena("token-usado", "Nueva2026")).thenReturn(false);

        mockMvc.perform(post("/login/restablecer").with(csrf()).param("token", "token-usado").param("nuevaContrasena", "Nueva2026"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("estado", "invalido"));
    }
}
