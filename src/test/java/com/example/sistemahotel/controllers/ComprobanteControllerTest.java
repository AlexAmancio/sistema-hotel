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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.sistemahotel.interfaces.IComprobanteService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Registro;

// Cubre el bug real encontrado en sesion: /comprobantes/emitir/{id}/ no
// revisaba el estado del registro. El comprobante congela el total (tarifa +
// consumos) del momento en que se emite y nunca se vuelve a recalcular, asi
// que emitirlo antes del check-out (o para una reserva cancelada/no-show)
// dejaba un total incompleto o directamente sin sentido, para siempre.
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "elon", roles = "ADMIN")
class ComprobanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IComprobanteService comprobanteService;

    @MockBean
    private IRegistroService registroService;

    @Test
    void rechazaEmitirComprobanteSiLaEstadiaNoTieneCheckoutHecho() throws Exception {
        Registro registro = new Registro();
        registro.setId(6);
        registro.setEstado(EstadoRegistro.ACTIVA);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));

        mockMvc.perform(post("/comprobantes/emitir/6/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error",
                "No se puede emitir comprobante: la estadía todavía no tiene check-out hecho."));

        verify(comprobanteService, never()).emitir(any());
    }

    @Test
    void emiteComprobanteCuandoLaEstadiaYaTieneCheckoutHecho() throws Exception {
        Registro registro = new Registro();
        registro.setId(6);
        registro.setEstado(EstadoRegistro.FINALIZADA);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));

        mockMvc.perform(post("/comprobantes/emitir/6/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/comprobantes/ver/6/"));

        verify(comprobanteService).emitir(registro);
    }
}
