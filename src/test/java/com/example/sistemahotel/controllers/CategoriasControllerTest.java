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
import org.springframework.security.test.context.support.WithMockUser;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Habitacion;

// Categorías de habitación (HU20 a HU23) y reglas RN10, RN14 y RN15.
@WithMockUser(username = "elon", roles = "ADMIN")
class CategoriasControllerTest extends ControladorIntegracionBase {

    private Categoria categoria(int id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        c.setPrecio(180.0);
        return c;
    }

    @Test
    void laListaDeCategoriasSeMuestra() throws Exception {
        when(categoriaService.listar()).thenReturn(List.of(categoria(1, "Habitación Matrimonial")));

        mockMvc.perform(get("/categorias/")).andExpect(status().isOk());
    }

    @Test
    void unaCategoriaConPrecioCeroSeRechaza() throws Exception {
        mockMvc.perform(post("/categorias/guardar/").with(csrf()).param("nombre", "Nueva").param("precio", "0"))
            .andExpect(redirectedUrl("/categorias/nuevo/"))
            .andExpect(flash().attribute("error", "El precio debe ser mayor a cero."));
        verify(categoriaService, never()).guardar(any());
    }

    @Test
    void unaTemporadaAltaQueTerminaAntesDeEmpezarSeRechaza() throws Exception {
        mockMvc.perform(post("/categorias/guardar/").with(csrf()).param("nombre", "Nueva").param("precio", "100")
                .param("precioTemporadaAlta", "150").param("temporadaAltaInicio", "2027-03-20").param("temporadaAltaFin", "2027-03-10"))
            .andExpect(redirectedUrl("/categorias/nuevo/"))
            .andExpect(flash().attribute("error", "La fecha de fin de temporada alta debe ser posterior a la de inicio."));
        verify(categoriaService, never()).guardar(any());
    }

    @Test
    void unNombreDeCategoriaRepetidoSeRechaza() throws Exception {
        when(categoriaService.consultarPorNombrecategoria("Habitación Matrimonial")).thenReturn(Optional.of(categoria(1, "Habitación Matrimonial")));

        mockMvc.perform(post("/categorias/guardar/").with(csrf()).param("nombre", "Habitación Matrimonial").param("precio", "200"))
            .andExpect(redirectedUrl("/categorias/nuevo/"))
            .andExpect(flash().attribute("error", "No se pudo guardar la categoría. Nombre repetido."));
    }

    @Test
    void alEditarSePuedeConservarElMismoNombre() throws Exception {
        when(categoriaService.consultarPorNombrecategoria("Habitación Matrimonial")).thenReturn(Optional.of(categoria(1, "Habitación Matrimonial")));

        mockMvc.perform(post("/categorias/guardar/").with(csrf()).param("id", "1").param("nombre", "Habitación Matrimonial").param("precio", "190"))
            .andExpect(redirectedUrl("/categorias/"))
            .andExpect(flash().attribute("exito", "Categoría actualizada correctamente."));
        verify(categoriaService).guardar(any());
    }

    @Test
    void crearUnaCategoriaConTemporadaAltaValida() throws Exception {
        mockMvc.perform(post("/categorias/guardar/").with(csrf()).param("nombre", "Domo Familiar").param("precio", "300")
                .param("precioTemporadaAlta", "380").param("temporadaAltaInicio", "2026-12-20").param("temporadaAltaFin", "2027-01-05"))
            .andExpect(redirectedUrl("/categorias/"))
            .andExpect(flash().attribute("exito", "Categoría creada correctamente."));
    }

    @Test
    void noSePuedeEliminarUnaCategoriaQueUsanHabitaciones() throws Exception {
        Habitacion habitacion = new Habitacion();
        habitacion.setCategoria(categoria(1, "Habitación Matrimonial"));
        when(habitacionService.listar()).thenReturn(List.of(habitacion));

        mockMvc.perform(post("/categorias/eliminar/1/").with(csrf()))
            .andExpect(redirectedUrl("/categorias/"))
            .andExpect(flash().attribute("error", "No se puede eliminar: hay habitaciones que usan esta categoría."));
        verify(categoriaService, never()).eliminar(anyInt());
    }

    @Test
    void eliminarUnaCategoriaSinHabitaciones() throws Exception {
        when(habitacionService.listar()).thenReturn(List.of());

        mockMvc.perform(post("/categorias/eliminar/9/").with(csrf()))
            .andExpect(redirectedUrl("/categorias/"))
            .andExpect(flash().attribute("exito", "Categoría eliminada correctamente."));
        verify(categoriaService).eliminar(9);
    }
}
