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

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

// Habitaciones (HU24 a HU30) y reglas RN04, RN10, RN14 y RN18.
@WithMockUser(username = "elon", roles = "ADMIN")
class HabitacionesControllerTest extends ControladorIntegracionBase {

    private Habitacion habitacion(int id, String numero) {
        Categoria categoria = new Categoria();
        categoria.setId(1);
        categoria.setNombre("Habitación Matrimonial");
        categoria.setPrecio(100.0);
        categoria.setPrecioTemporadaAlta(150.0);
        Calendar ini = Calendar.getInstance(); ini.clear(); ini.set(2027, Calendar.MARCH, 10);
        Calendar fin = Calendar.getInstance(); fin.clear(); fin.set(2027, Calendar.MARCH, 20);
        categoria.setTemporadaAltaInicio(ini.getTime());
        categoria.setTemporadaAltaFin(fin.getTime());
        Habitacion h = new Habitacion();
        h.setId(id);
        h.setNumero(numero);
        h.setPiso(1);
        h.setAforo(2);
        h.setCategoria(categoria);
        return h;
    }

    @Test
    void laListaDeHabitacionesSeMuestra() throws Exception {
        when(habitacionService.listar()).thenReturn(List.of(habitacion(1, "101")));

        mockMvc.perform(get("/habitaciones/")).andExpect(status().isOk());
    }

    @Test
    void unNumeroDeHabitacionRepetidoSeRechaza() throws Exception {
        when(habitacionService.consultarPornumerohabitacion("101")).thenReturn(Optional.of(habitacion(1, "101")));

        mockMvc.perform(post("/habitaciones/guardar/").with(csrf()).param("numero", "101").param("piso", "1").param("aforo", "2").param("estado", "DISPONIBLE"))
            .andExpect(redirectedUrl("/habitaciones/nuevo/"))
            .andExpect(flash().attribute("error", "No se pudo guardar la habitación. N° de habitación repetido."));
        verify(habitacionService, never()).guardar(any());
    }

    @Test
    void crearUnaHabitacionConNumeroNuevo() throws Exception {
        mockMvc.perform(post("/habitaciones/guardar/").with(csrf()).param("numero", "991").param("piso", "9").param("aforo", "2").param("estado", "DISPONIBLE"))
            .andExpect(redirectedUrl("/habitaciones/"))
            .andExpect(flash().attribute("exito", "Habitación creada correctamente."));
        verify(habitacionService).guardar(any());
    }

    @Test
    void noSePuedeEliminarUnaHabitacionConRegistros() throws Exception {
        Registro registro = new Registro();
        registro.setHabitacion(habitacion(1, "101"));
        when(registroService.listar()).thenReturn(List.of(registro));

        mockMvc.perform(post("/habitaciones/eliminar/1/").with(csrf()))
            .andExpect(redirectedUrl("/habitaciones/"))
            .andExpect(flash().attribute("error", "No se puede eliminar: esta habitación tiene registros asociados."));
        verify(habitacionService, never()).eliminar(anyInt());
    }

    @Test
    void eliminarUnaHabitacionSinRegistros() throws Exception {
        when(registroService.listar()).thenReturn(List.of());

        mockMvc.perform(post("/habitaciones/eliminar/5/").with(csrf()))
            .andExpect(redirectedUrl("/habitaciones/"))
            .andExpect(flash().attribute("exito", "Habitación eliminada correctamente."));
        verify(habitacionService).eliminar(5);
    }

    @Test
    void marcarDisponibleDejaLaHabitacionLista() throws Exception {
        Habitacion enLimpieza = habitacion(1, "101");
        enLimpieza.setEstado(EstadoHabitacion.LIMPIEZA);
        when(habitacionService.consultarId(1)).thenReturn(Optional.of(enLimpieza));

        mockMvc.perform(post("/habitaciones/marcar-disponible/1/").with(csrf()))
            .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(EstadoHabitacion.DISPONIBLE, enLimpieza.getEstado());
        verify(habitacionService).guardar(enLimpieza);
    }

    @Test
    void consultarDisponiblesConFechaInvalidaDevuelveError() throws Exception {
        mockMvc.perform(get("/habitaciones/disponibles").param("checkIn", "no-es-fecha").param("noches", "1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void consultarDisponiblesDevuelveLasHabitacionesLibres() throws Exception {
        when(registroService.obtenerHabitacionesDisponibles(any(), org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.isNull()))
            .thenReturn(List.of(habitacion(2, "102")));

        mockMvc.perform(get("/habitaciones/disponibles").param("checkIn", "2027-03-01T12:00").param("noches", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].numero").value("102"));
    }

    @Test
    void laTarifaDeUnaHabitacionInexistenteDevuelve404() throws Exception {
        when(habitacionService.consultarId(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/habitaciones/calcular-tarifa/99").param("noches", "2"))
            .andExpect(status().isNotFound());
    }

    @Test
    void laTarifaFueraDeTemporadaUsaElPrecioBase() throws Exception {
        when(habitacionService.consultarId(1)).thenReturn(Optional.of(habitacion(1, "101")));

        mockMvc.perform(get("/habitaciones/calcular-tarifa/1").param("noches", "2").param("checkIn", "2027-03-01T12:00"))
            .andExpect(status().isOk())
            .andExpect(content().string("200.0"));
    }

    @Test
    void laTarifaEnTemporadaAltaUsaElPrecioAlto() throws Exception {
        when(habitacionService.consultarId(1)).thenReturn(Optional.of(habitacion(1, "101")));

        mockMvc.perform(get("/habitaciones/calcular-tarifa/1").param("noches", "2").param("checkIn", "2027-03-12T12:00"))
            .andExpect(status().isOk())
            .andExpect(content().string("300.0"));
    }
}
