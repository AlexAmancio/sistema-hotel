package com.example.sistemahotel.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IProductoConsumoService;

// Catálogo de productos de consumo (minibar, room service, lavandería...)
// pedido para reemplazar la descripción libre tipeada a mano en cada
// consumo por un registro real en SQL, reutilizable como atajo.
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "elon", roles = "ADMIN")
class ProductosConsumoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IProductoConsumoService service;

    @MockBean
    private IConsumoService consumoService;

    @Test
    void rechazaGuardarSinNombre() throws Exception {
        mockMvc.perform(post("/productos-consumo/guardar/")
                .with(csrf())
                .param("id", "0")
                .param("nombre", "")
                .param("precioSugerido", "20"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/productos-consumo/nuevo/"))
            .andExpect(flash().attribute("error", "El nombre es obligatorio."));

        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaGuardarConPrecioNegativo() throws Exception {
        mockMvc.perform(post("/productos-consumo/guardar/")
                .with(csrf())
                .param("id", "0")
                .param("nombre", "Minibar")
                .param("precioSugerido", "-5"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/productos-consumo/nuevo/"))
            .andExpect(flash().attribute("error", "El precio sugerido no puede ser negativo."));

        verify(service, never()).guardar(any());
    }

    @Test
    void guardaCuandoLosDatosSonValidos() throws Exception {
        mockMvc.perform(post("/productos-consumo/guardar/")
                .with(csrf())
                .param("id", "0")
                .param("nombre", "Minibar")
                .param("precioSugerido", "20"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/productos-consumo/"));

        verify(service).guardar(any());
    }

    // Bug evitado a propósito: eliminar un producto que ya se usó en algún
    // consumo rompería la integridad del historial (ese consumo quedaría
    // apuntando a un producto_consumo_id inexistente).
    @Test
    void rechazaEliminarSiYaHayConsumosConEseProducto() throws Exception {
        when(consumoService.existeConsumoConProducto(2)).thenReturn(true);

        mockMvc.perform(post("/productos-consumo/eliminar/2/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/productos-consumo/"))
            .andExpect(flash().attribute("error",
                "No se puede eliminar: hay consumos ya cobrados que usan este producto. Desactivalo en vez de eliminarlo."));

        verify(service, never()).eliminar(2);
    }

    @Test
    void eliminaCuandoNoHayConsumosConEseProducto() throws Exception {
        when(consumoService.existeConsumoConProducto(2)).thenReturn(false);

        mockMvc.perform(post("/productos-consumo/eliminar/2/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/productos-consumo/"));

        verify(service).eliminar(2);
    }
}
