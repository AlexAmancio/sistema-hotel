package com.example.sistemahotel.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
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
import com.example.sistemahotel.interfaces.IRegistroService;
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

    @Test
    void rechazaMontoVacioConMensajeClaroEnVezDeError500() throws Exception {
        mockMvc.perform(post("/consumos/agregar/6/")
                .with(csrf())
                .param("descripcion", "Minibar")
                .param("monto", ""))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/consumos/6/"))
            .andExpect(flash().attribute("error", "El monto debe ser mayor a cero."));

        verify(consumoService, never()).agregar(any(), anyString(), anyDouble());
    }

    @Test
    void rechazaMontoNegativoOCero() throws Exception {
        mockMvc.perform(post("/consumos/agregar/6/")
                .with(csrf())
                .param("descripcion", "Minibar")
                .param("monto", "0"))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attribute("error", "El monto debe ser mayor a cero."));

        verify(consumoService, never()).agregar(any(), anyString(), anyDouble());
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

        verify(consumoService).agregar(registro, "Minibar", 35.0);
    }
}
