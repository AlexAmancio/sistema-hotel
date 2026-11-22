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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

// Pantallas de consulta: búsqueda global (HU62), ocupación (HU63), reportes
// (HU64 y HU65) y vista del comprobante (HU61).
@WithMockUser(username = "elon", roles = "ADMIN")
class ConsultasControllerTest extends ControladorIntegracionBase {

    @Test
    void laBusquedaSinTextoNoConsultaNada() throws Exception {
        mockMvc.perform(get("/buscar")).andExpect(status().isOk());
        verify(clienteService, never()).buscar(any());
    }

    @Test
    void laBusquedaConsultaClientesHabitacionesYRegistros() throws Exception {
        when(clienteService.buscar("210")).thenReturn(List.of());
        when(habitacionService.buscar("210")).thenReturn(List.of());
        when(registroService.buscarPorNumeroHabitacion("210")).thenReturn(List.of());

        mockMvc.perform(get("/buscar").param("q", " 210 ")).andExpect(status().isOk());

        verify(clienteService).buscar("210");
        verify(habitacionService).buscar("210");
        verify(registroService).buscarPorNumeroHabitacion("210");
    }

    @Test
    void elCalendarioDeOcupacionSeMuestra() throws Exception {
        mockMvc.perform(get("/ocupacion/")).andExpect(status().isOk());
    }

    @Test
    void losReportesSeMuestran() throws Exception {
        mockMvc.perform(get("/reportes/")).andExpect(status().isOk());
    }

    @Test
    void elPanelDeIndicadoresSeMuestra() throws Exception {
        mockMvc.perform(get("/dashboard/")).andExpect(status().isOk());
    }

    @Test
    void verUnComprobanteQueNoExisteVuelveALaLista() throws Exception {
        when(comprobanteService.consultarPorRegistro(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/comprobantes/ver/99/")).andExpect(redirectedUrl("/"));
    }
}
