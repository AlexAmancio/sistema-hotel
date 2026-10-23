package com.example.sistemahotel.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IProductoConsumoService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Registro;

// Cubre el bug real encontrado en produccion: el endpoint aceptaba un monto
// vacio/invalido y explotaba con la pantalla blanca de error de Spring en vez
// de redirigir con un mensaje claro.
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "elon", roles = "ADMIN")
class ConsumoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IConsumoService consumoService;

    @MockBean
    private IRegistroService registroService;

    @MockBean
    private IProductoConsumoService productoConsumoService;

    @Test
    void rechazaMontoVacioConMensajeClaroEnVezDeError500() throws Exception {
        mockMvc.perform(post("/consumos/agregar/6/")
                .with(csrf())
                .param("descripcion", "Minibar")
                .param("monto", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/consumos/6/"))
            .andExpect(flash().attribute("error", "El monto debe ser mayor a cero."));

        verify(consumoService, never()).agregar(any(), anyString(), anyDouble(), any());
    }

    @Test
    void rechazaMontoNegativoOCero() throws Exception {
        mockMvc.perform(post("/consumos/agregar/6/")
                .with(csrf())
                .param("descripcion", "Minibar")
                .param("monto", "0"))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attribute("error", "El monto debe ser mayor a cero."));

        verify(consumoService, never()).agregar(any(), anyString(), anyDouble(), any());
    }

    @Test
    void agregaElConsumoCuandoLosDatosSonValidos() throws Exception {
        Registro registro = new Registro();
        registro.setId(6);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));

        mockMvc.perform(post("/consumos/agregar/6/")
                .with(csrf())
                .param("descripcion", "Minibar")
                .param("monto", "35"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/consumos/6/"))
            .andExpect(flash().attribute("exito", "Consumo agregado correctamente."));

        verify(consumoService).agregar(registro, "Minibar", 35.0, null);
    }

    @Test
    void agregaElConsumoEnlazadoAlProductoCuandoVieneDeUnAtajo() throws Exception {
        Registro registro = new Registro();
        registro.setId(6);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));
        com.example.sistemahotel.modelos.ProductoConsumo producto = new com.example.sistemahotel.modelos.ProductoConsumo();
        producto.setId(2);
        producto.setNombre("Minibar");
        when(productoConsumoService.consultarId(2)).thenReturn(Optional.of(producto));

        mockMvc.perform(post("/consumos/agregar/6/")
                .with(csrf())
                .param("descripcion", "Minibar")
                .param("monto", "35")
                .param("productoId", "2"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/consumos/6/"));

        verify(consumoService).agregar(registro, "Minibar", 35.0, producto);
    }

    // Bug real encontrado en sesión: el formulario se ocultaba en el HTML si
    // la estadía no estaba activa, pero el endpoint igual aceptaba un POST
    // directo y agregaba el consumo igual.
    @Test
    void rechazaAgregarConsumoSiLaEstadiaNoEstaActiva() throws Exception {
        Registro registro = new Registro();
        registro.setId(6);
        registro.setEstado(EstadoRegistro.FINALIZADA);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));

        mockMvc.perform(post("/consumos/agregar/6/")
                .with(csrf())
                .param("descripcion", "Minibar")
                .param("monto", "35"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/consumos/6/"))
            .andExpect(flash().attribute("error",
                "Esta estadía ya no está activa — no se pueden agregar más consumos."));

        verify(consumoService, never()).agregar(any(), anyString(), anyDouble(), any());
    }

    @Test
    void eliminaElConsumoCuandoLaEstadiaEstaActiva() throws Exception {
        Registro registro = new Registro();
        registro.setId(6);
        registro.setEstado(EstadoRegistro.ACTIVA);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));

        mockMvc.perform(post("/consumos/eliminar/6/14/")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/consumos/6/"));

        verify(consumoService).eliminar(14);
    }

    // Mismo bug que en agregar(), pero para eliminar: el botón se ocultaba en
    // el HTML si la estadía no estaba activa, pero el endpoint viejo
    // (/consumos/eliminar/{consumoID}/, sin el registroID) no tenía forma de
    // verificarlo y borraba igual con un POST directo.
    @Test
    void rechazaEliminarConsumoSiLaEstadiaNoEstaActiva() throws Exception {
        Registro registro = new Registro();
        registro.setId(6);
        registro.setEstado(EstadoRegistro.FINALIZADA);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));

        mockMvc.perform(post("/consumos/eliminar/6/14/")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/consumos/6/"))
            .andExpect(flash().attribute("error",
                "Esta estadía ya no está activa — no se pueden quitar consumos de su historial."));

        verify(consumoService, never()).eliminar(anyInt());
    }
}
