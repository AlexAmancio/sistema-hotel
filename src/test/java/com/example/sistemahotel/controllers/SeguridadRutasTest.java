package com.example.sistemahotel.controllers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

// Regla RN35: toda página interna exige sesión iniciada; solo el administrador
// crea, edita o elimina usuarios y productos; el personal de recepción solo los
// consulta. También verifica que las acciones exijan el token CSRF.
class SeguridadRutasTest extends ControladorIntegracionBase {

    @Test
    @WithAnonymousUser
    void sinSesionUnaPaginaInternaRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/clientes/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithAnonymousUser
    void elLoginEsPublico() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void laRecuperacionDeContrasenaEsPublica() throws Exception {
        mockMvc.perform(get("/login/olvide-contrasena")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "mquispe", roles = "STAFF")
    void recepcionNoPuedeAbrirElFormularioDeNuevoUsuario() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo/")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mquispe", roles = "STAFF")
    void recepcionNoPuedeEliminarUsuarios() throws Exception {
        mockMvc.perform(post("/usuarios/eliminar/2/").with(csrf())).andExpect(status().isForbidden());
        verify(usuarioService, never()).eliminar(anyInt());
    }

    @Test
    @WithMockUser(username = "mquispe", roles = "STAFF")
    void recepcionNoPuedeCrearProductosDelCatalogo() throws Exception {
        mockMvc.perform(get("/productos-consumo/nuevo/")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mquispe", roles = "STAFF")
    void recepcionSiPuedeConsultarLaListaDeUsuarios() throws Exception {
        mockMvc.perform(get("/usuarios/")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "mquispe", roles = "STAFF")
    void recepcionSiPuedeConsultarElCatalogoDeProductos() throws Exception {
        mockMvc.perform(get("/productos-consumo/")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "elon", roles = "ADMIN")
    void elAdministradorPuedeAbrirElFormularioDeNuevoUsuario() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo/")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "elon", roles = "ADMIN")
    void unaAccionSinTokenCsrfNoSeEjecuta() throws Exception {
        mockMvc.perform(post("/clientes/eliminar/1/")).andExpect(status().is3xxRedirection());
        verify(clienteService, never()).eliminar(anyInt());
    }
}
