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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.TipoDocumento;

// Clientes (HU14 a HU19) y reglas RN10, RN14, RN29, RN40 y RN41.
@WithMockUser(username = "elon", roles = "ADMIN")
class ClientesControllerTest extends ControladorIntegracionBase {

    private Cliente cliente(int id, String dni) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombres("Carmen");
        c.setApellidos("Herrera Rojas");
        c.setIdentificacion(dni);
        return c;
    }

    @Test
    void laListaDeClientesSeMuestraPaginada() throws Exception {
        when(clienteService.listarPaginado(any())).thenReturn(new PageImpl<>(List.of(cliente(1, "79257439"))));

        mockMvc.perform(get("/clientes/"))
            .andExpect(status().isOk());
    }

    @Test
    void registrarUnClienteConDocumentoRepetidoSeRechaza() throws Exception {
        when(clienteService.consultarPorIdentificacion("79257439")).thenReturn(Optional.of(cliente(1, "79257439")));

        mockMvc.perform(post("/clientes/guardar/").with(csrf())
                .param("nombres", "Otra").param("apellidos", "Persona").param("identificacion", "79257439").param("tipoDocumento", "DNI"))
            .andExpect(redirectedUrl("/clientes/nuevo/"))
            .andExpect(flash().attribute("error", "No se pudo registrar el cliente. Ya existe un cliente con esa identificación."));
        verify(clienteService, never()).guardar(any());
    }

    @Test
    void registrarUnClienteGuardaNombresYCiudadConMayusculaInicial() throws Exception {
        when(clienteService.listarPaginado(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post("/clientes/guardar/").with(csrf())
                .param("nombres", "cLIENTE wEB").param("apellidos", "prueba navegador").param("identificacion", "99000011")
                .param("tipoDocumento", "DNI").param("ciudad", "oxapampa").param("pais", "perú"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("exito", "Cliente registrado correctamente."));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteService).guardar(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("Cliente Web", captor.getValue().getNombres());
        org.junit.jupiter.api.Assertions.assertEquals("Oxapampa", captor.getValue().getCiudad());
    }

    @Test
    void noSePuedeEliminarUnClienteConRegistros() throws Exception {
        Registro registro = new Registro();
        registro.setCliente(cliente(1, "79257439"));
        when(registroService.listar()).thenReturn(List.of(registro));

        mockMvc.perform(post("/clientes/eliminar/1/").with(csrf()))
            .andExpect(redirectedUrl("/clientes/"))
            .andExpect(flash().attribute("error", "No se puede eliminar: este cliente tiene registros asociados."));
        verify(clienteService, never()).eliminar(anyInt());
    }

    @Test
    void eliminarUnClienteSinRegistros() throws Exception {
        when(registroService.listar()).thenReturn(List.of());

        mockMvc.perform(post("/clientes/eliminar/5/").with(csrf()))
            .andExpect(redirectedUrl("/clientes/"))
            .andExpect(flash().attribute("exito", "Cliente eliminado correctamente."));
        verify(clienteService).eliminar(5);
    }

    @Test
    void elAltaRapidaExigeNombresApellidosEIdentificacion() throws Exception {
        mockMvc.perform(post("/clientes/crear-rapido/").with(csrf())
                .param("nombres", "Rápido").param("apellidos", "").param("identificacion", "99000002"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Nombres, apellidos e identificación son obligatorios."));
    }

    @Test
    void elAltaRapidaRechazaUnDocumentoYaRegistrado() throws Exception {
        when(clienteService.consultarPorIdentificacion("79257439")).thenReturn(Optional.of(cliente(1, "79257439")));

        mockMvc.perform(post("/clientes/crear-rapido/").with(csrf())
                .param("nombres", "Rápido").param("apellidos", "QA").param("identificacion", "79257439"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Ya existe un cliente con esa identificación."));
    }

    @Test
    void elAltaRapidaCreaElClienteYDevuelveSuNombre() throws Exception {
        mockMvc.perform(post("/clientes/crear-rapido/").with(csrf())
                .param("nombres", "rápido").param("apellidos", "qa").param("identificacion", " 99000002 ").param("tipoDocumento", "PASAPORTE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Rápido Qa"));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteService).guardar(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("99000002", captor.getValue().getIdentificacion());
        org.junit.jupiter.api.Assertions.assertEquals(TipoDocumento.PASAPORTE, captor.getValue().getTipoDocumento());
    }

    @Test
    void elAltaRapidaUsaDniSiElTipoDeDocumentoNoEsValido() throws Exception {
        mockMvc.perform(post("/clientes/crear-rapido/").with(csrf())
                .param("nombres", "Rápido").param("apellidos", "QA").param("identificacion", "99000003").param("tipoDocumento", "OTRO"))
            .andExpect(status().isOk());

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteService).guardar(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(TipoDocumento.DNI, captor.getValue().getTipoDocumento());
    }

    @Test
    void elAutocompletadoRechazaUnDniQueNoTieneOchoDigitos() throws Exception {
        mockMvc.perform(get("/clientes/dni/1234"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("DNI inválido")));
    }
}
