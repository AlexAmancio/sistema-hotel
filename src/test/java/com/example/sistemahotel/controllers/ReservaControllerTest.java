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
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Reserva;
import com.example.sistemahotel.modelos.Usuario;

// Reserva múltiple (HU47 a HU49) y reglas RN02, RN03, RN30 y RN42.
@WithMockUser(username = "elon", roles = "ADMIN")
class ReservaControllerTest extends ControladorIntegracionBase {

    private Cliente cliente;
    private Usuario elon;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1);
        cliente.setNombres("Karen");
        cliente.setApellidos("Díaz Pérez");
        elon = new Usuario();
        elon.setId(1);
        elon.setUsername("elon");
        when(clienteService.consultarId(1)).thenReturn(Optional.of(cliente));
        when(usuarioService.consultarpornombreusuario("elon")).thenReturn(Optional.of(elon));
    }

    @Test
    void elFormularioDeReservaSeMuestra() throws Exception {
        mockMvc.perform(get("/reservas/nueva/")).andExpect(status().isOk());
    }

    @Test
    void unaReservaValidaRedirigeAlDetalle() throws Exception {
        Reserva reserva = new Reserva();
        reserva.setId(5);
        when(reservaService.crear(any(), any(), any(), anyInt(), any(), any())).thenReturn(reserva);

        mockMvc.perform(post("/reservas/guardar/").with(csrf()).param("cliente", "1").param("checkIn", "2027-01-15T14:00").param("noches", "2")
                .param("habitaciones", "1", "2").param("huespedes_1", "2").param("huespedes_2", "1"))
            .andExpect(redirectedUrl("/reservas/5/"));
    }

    @Test
    void laReservaQuedaANombreDelUsuarioEnSesionConLosHuespedesPorHabitacion() throws Exception {
        Reserva reserva = new Reserva();
        reserva.setId(5);
        when(reservaService.crear(any(), any(), any(), anyInt(), any(), any())).thenReturn(reserva);

        mockMvc.perform(post("/reservas/guardar/").with(csrf()).param("cliente", "1").param("checkIn", "2027-01-15T14:00").param("noches", "2")
                .param("habitaciones", "1", "2").param("huespedes_1", "2").param("huespedes_2", "abc"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, Integer>> huespedes = ArgumentCaptor.forClass(Map.class);
        verify(reservaService).crear(org.mockito.ArgumentMatchers.eq(cliente), org.mockito.ArgumentMatchers.eq(elon), any(),
            org.mockito.ArgumentMatchers.eq(2), huespedes.capture(), org.mockito.ArgumentMatchers.eq(List.of(1, 2)));
        org.junit.jupiter.api.Assertions.assertEquals(Map.of(1, 2, 2, 1), huespedes.getValue());
    }

    @Test
    void siElServicioRechazaLaReservaSeMuestraElMotivo() throws Exception {
        when(reservaService.crear(any(), any(), any(), anyInt(), any(), any()))
            .thenThrow(new IllegalArgumentException("Elegí al menos una habitación para la reserva."));

        mockMvc.perform(post("/reservas/guardar/").with(csrf()).param("cliente", "1").param("checkIn", "2027-01-15T14:00").param("noches", "2"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("error", "Elegí al menos una habitación para la reserva."));
    }

    @Test
    void unaFechaDeIngresoInvalidaSeRechaza() throws Exception {
        mockMvc.perform(post("/reservas/guardar/").with(csrf()).param("cliente", "1").param("checkIn", "15/01/2027").param("noches", "2")
                .param("habitaciones", "1"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("error", "La fecha de ingreso no es válida."));
        verify(reservaService, never()).crear(any(), any(), any(), anyInt(), any(), any());
    }
}
