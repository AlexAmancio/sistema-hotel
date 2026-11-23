package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Gestión de usuarios (HU09 a HU13) y reglas RN12, RN14, RN27 y RN35.
@WithMockUser(username = "elon", roles = "ADMIN")
class UsuariosControllerTest extends ControladorIntegracionBase {

    private Usuario usuario(int id, String username, boolean esAdmin) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setNombres("Nombre");
        u.setApellidos("Apellido");
        u.setEsAdmin(esAdmin);
        u.setActivo(true);
        return u;
    }

    @Test
    void laListaDeUsuariosSeMuestra() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(usuario(1, "elon", true)));

        mockMvc.perform(get("/usuarios/"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("listaUsuarios"));
    }

    @Test
    void crearUnUsuarioSinContrasenaSeRechaza() throws Exception {
        mockMvc.perform(post("/usuarios/guardar/").with(csrf())
                .param("username", "qa").param("nombres", "Prueba").param("apellidos", "QA").param("contrasena", ""))
            .andExpect(redirectedUrl("/usuarios/nuevo/"))
            .andExpect(flash().attribute("error", "La contraseña es obligatoria para un nuevo usuario"));
        verify(usuarioService, never()).guardar(any());
    }

    @Test
    void crearUnUsuarioConUsernameRepetidoSeRechaza() throws Exception {
        when(usuarioService.consultarpornombreusuario("mquispe")).thenReturn(Optional.of(usuario(2, "mquispe", false)));

        mockMvc.perform(post("/usuarios/guardar/").with(csrf())
                .param("username", "mquispe").param("nombres", "Otra").param("apellidos", "Persona").param("contrasena", "Clave2026"))
            .andExpect(redirectedUrl("/usuarios/nuevo/"))
            .andExpect(flash().attribute("error", "No se pudo registrar al usuario, username repetido"));
        verify(usuarioService, never()).guardar(any());
    }

    @Test
    void crearUnUsuarioValidoGuardaLaContrasenaCifradaYLosNombresConMayuscula() throws Exception {
        mockMvc.perform(post("/usuarios/guardar/").with(csrf())
                .param("username", "qa").param("nombres", "prueba web").param("apellidos", "navegador").param("contrasena", "Clave2026"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("exito", "Usuario creado correctamente."));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).guardar(captor.capture());
        assertTrue(captor.getValue().getContrasena().startsWith("$2a$"));
        assertTrue("Prueba Web".equals(captor.getValue().getNombres()));
    }

    @Test
    void editarSinEscribirContrasenaConservaLaAnterior() throws Exception {
        Usuario existente = usuario(2, "mquispe", false);
        existente.setContrasena("$2a$10$hashAnterior");
        when(usuarioService.consultarId(2)).thenReturn(Optional.of(existente));

        mockMvc.perform(post("/usuarios/guardar/").with(csrf())
                .param("id", "2").param("username", "mquispe").param("nombres", "María").param("apellidos", "Quispe").param("contrasena", ""))
            .andExpect(status().isOk())
            .andExpect(model().attribute("exito", "Usuario actualizado correctamente."));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).guardar(captor.capture());
        assertTrue("$2a$10$hashAnterior".equals(captor.getValue().getContrasena()));
    }

    @Test
    void noSePuedeEliminarAlUnicoAdministrador() throws Exception {
        Usuario admin = usuario(1, "elon", true);
        when(usuarioService.consultarId(1)).thenReturn(Optional.of(admin));
        when(usuarioService.listar()).thenReturn(List.of(admin, usuario(2, "mquispe", false)));

        mockMvc.perform(post("/usuarios/eliminar/1/").with(csrf()))
            .andExpect(redirectedUrl("/usuarios/"))
            .andExpect(flash().attribute("error", "No se puede eliminar al único administrador del sistema."));
        verify(usuarioService, never()).eliminar(anyInt());
    }

    @Test
    void noSePuedeDesactivarAlUnicoAdministrador() throws Exception {
        Usuario admin = usuario(1, "elon", true);
        when(usuarioService.consultarId(1)).thenReturn(Optional.of(admin));
        when(usuarioService.listar()).thenReturn(List.of(admin));

        mockMvc.perform(post("/usuarios/desactivar/1/").with(csrf()))
            .andExpect(redirectedUrl("/usuarios/"))
            .andExpect(flash().attribute("error", "No se puede desactivar al único administrador del sistema."));
        assertTrue(admin.isActivo());
    }

    @Test
    void noSePuedeEliminarUnUsuarioConRegistros() throws Exception {
        Usuario staff = usuario(2, "mquispe", false);
        Registro registro = new Registro();
        registro.setUsuario(staff);
        when(usuarioService.consultarId(2)).thenReturn(Optional.of(staff));
        when(registroService.listar()).thenReturn(List.of(registro));

        mockMvc.perform(post("/usuarios/eliminar/2/").with(csrf()))
            .andExpect(redirectedUrl("/usuarios/"))
            .andExpect(flash().attribute("error", "No se puede eliminar: este usuario tiene registros asociados. Desactivalo en su lugar."));
        verify(usuarioService, never()).eliminar(anyInt());
    }

    @Test
    void eliminarUnUsuarioSinRegistros() throws Exception {
        when(usuarioService.consultarId(3)).thenReturn(Optional.of(usuario(3, "qa", false)));
        when(registroService.listar()).thenReturn(List.of());

        mockMvc.perform(post("/usuarios/eliminar/3/").with(csrf()))
            .andExpect(redirectedUrl("/usuarios/"))
            .andExpect(flash().attribute("exito", "Usuario eliminado correctamente."));
        verify(usuarioService).eliminar(3);
    }

    @Test
    void desactivarUnUsuarioLoDejaInactivo() throws Exception {
        Usuario staff = usuario(3, "qa", false);
        when(usuarioService.consultarId(3)).thenReturn(Optional.of(staff));

        mockMvc.perform(post("/usuarios/desactivar/3/").with(csrf()))
            .andExpect(redirectedUrl("/usuarios/"))
            .andExpect(flash().attribute("exito", "Usuario desactivado correctamente."));
        assertFalse(staff.isActivo());
        verify(usuarioService).guardar(staff);
    }

    @Test
    void activarUnUsuarioLoDejaActivo() throws Exception {
        Usuario staff = usuario(3, "qa", false);
        staff.setActivo(false);
        when(usuarioService.consultarId(3)).thenReturn(Optional.of(staff));

        mockMvc.perform(post("/usuarios/activar/3/").with(csrf()))
            .andExpect(redirectedUrl("/usuarios/"));
        assertTrue(staff.isActivo());
    }

    @Test
    void editarUnUsuarioInexistenteVuelveALaLista() throws Exception {
        when(usuarioService.consultarId(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/usuarios/editar/99/"))
            .andExpect(redirectedUrl("/usuarios/"));
    }
}
