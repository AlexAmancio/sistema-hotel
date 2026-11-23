package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Reserva;
import com.example.sistemahotel.modelos.Usuario;

// Casos límite de los controladores con los servicios simulados: edición
// simultánea (bloqueo optimista, RN16), registros que ya no existen, parámetros
// inválidos o ausentes y valores por defecto del formulario de reserva.
@WithMockUser(username = "elon", roles = {"ADMIN", "STAFF"})
class CasosLimiteControllerTest extends ControladorIntegracionBase {

    private static final String MODIFICADO = "Este registro fue modificado por otra persona justo ahora. Recargá la página e intentá de nuevo.";

    @Test
    void siOtraPersonaModificoLaEstadiaElCheckoutAvisaYNoSeRompe() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Registro.class, 1)).when(registroService).hacerCheckout(1);

        mockMvc.perform(post("/checkout/1/").with(csrf()))
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error", MODIFICADO));
    }

    @Test
    void siOtraPersonaModificoLaEstadiaLaCancelacionAvisa() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Registro.class, 1)).when(registroService).cancelar(1);

        mockMvc.perform(post("/cancelar/1/").with(csrf()))
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error", MODIFICADO));
    }

    @Test
    void siOtraPersonaModificoLaEstadiaElNoShowAvisa() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Registro.class, 1)).when(registroService).marcarNoShow(1);

        mockMvc.perform(post("/no-show/1/").with(csrf()))
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error", MODIFICADO));
    }

    @Test
    void agregarUnConsumoAUnaEstadiaQueNoExisteAvisa() throws Exception {
        mockMvc.perform(post("/consumos/agregar/99/").with(csrf()).param("descripcion", "Agua").param("monto", "5"))
            .andExpect(redirectedUrl("/consumos/99/"))
            .andExpect(flash().attribute("error", "No se encontró el registro."));
        verify(consumoService, never()).agregar(any(), any(), anyDouble(), any());
    }

    @Test
    void agregarUnConsumoSinDescripcionNiMontoSeRechaza() throws Exception {
        mockMvc.perform(post("/consumos/agregar/1/").with(csrf()).param("monto", "5"))
            .andExpect(flash().attribute("error", "La descripción del consumo es obligatoria."));
        mockMvc.perform(post("/consumos/agregar/1/").with(csrf()).param("descripcion", "Agua"))
            .andExpect(flash().attribute("error", "El monto debe ser mayor a cero."));
    }

    @Test
    void quitarUnConsumoDeUnaEstadiaQueNoExisteVuelveALaLista() throws Exception {
        mockMvc.perform(post("/consumos/eliminar/99/5/").with(csrf()))
            .andExpect(redirectedUrl("/"));
        verify(consumoService, never()).eliminar(anyInt());
    }

    @Test
    void eliminarODesactivarUnUsuarioQueNoExisteVuelveALaLista() throws Exception {
        mockMvc.perform(post("/usuarios/eliminar/99/").with(csrf())).andExpect(redirectedUrl("/usuarios/"));
        mockMvc.perform(post("/usuarios/desactivar/99/").with(csrf())).andExpect(redirectedUrl("/usuarios/"));
        verify(usuarioService, never()).eliminar(anyInt());
    }

    @Test
    void conUnEstadoInvalidoLaListaMuestraTodasLasEstadias() throws Exception {
        mockMvc.perform(get("/").param("estado", "INVENTADO"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("estadoSeleccionado", "INVENTADO"));
        verify(registroService).listarOrdenadoPorPrioridad(null);
    }

    @Test
    void unaPaginaFueraDeRangoSeAjustaALaUltima() throws Exception {
        mockMvc.perform(get("/").param("pagina", "50"))
            .andExpect(model().attribute("paginaActual", 0))
            .andExpect(model().attribute("totalPaginas", 1));
    }

    @Test
    void conUnaFechaInvalidaElCheckInNuevoQuedaSinFechaPrecargada() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/nuevo/").param("checkIn", "no-es-fecha"))
            .andExpect(status().isOk())
            .andReturn();

        assertNull(((Registro) resultado.getModelAndView().getModel().get("registro")).getCheckIn());
        assertEquals(1, ((Registro) resultado.getModelAndView().getModel().get("registro")).getNumHuespedes());
    }

    @Test
    void conUnaFechaInvalidaElCalendarioDeOcupacionArrancaHoy() throws Exception {
        String hoy = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        // Texto que no es fecha y parámetro vacío: en ambos casos arranca hoy.
        mockMvc.perform(get("/ocupacion/").param("desde", "no-es-fecha"))
            .andExpect(status().isOk())
            .andExpect(view().name("ocupacion"))
            .andExpect(model().attribute("desdeHoy", hoy));
        mockMvc.perform(get("/ocupacion/").param("desde", ""))
            .andExpect(model().attribute("desdeHoy", hoy));
    }

    @Test
    void enLaReservaUnNumeroDeHuespedesVacioONoNumericoCuentaComoUno() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        Usuario elon = new Usuario();
        elon.setUsername("elon");
        Reserva reserva = new Reserva();
        reserva.setId(5);
        when(clienteService.consultarId(1)).thenReturn(Optional.of(cliente));
        when(usuarioService.consultarpornombreusuario("elon")).thenReturn(Optional.of(elon));
        when(reservaService.crear(any(), any(), any(), anyInt(), anyMap(), any())).thenReturn(reserva);

        mockMvc.perform(post("/reservas/guardar/").with(csrf())
                .param("cliente", "1").param("checkIn", "2028-06-10T12:00").param("noches", "2")
                .param("habitaciones", "10", "11", "12")
                .param("huespedes_10", "").param("huespedes_11", "dos").param("huespedes_12", "3"))
            .andExpect(redirectedUrl("/reservas/5/"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, Integer>> huespedes = ArgumentCaptor.forClass(Map.class);
        verify(reservaService).crear(eq(cliente), eq(elon), any(), eq(2), huespedes.capture(), any());
        assertEquals(Map.of(10, 1, 11, 1, 12, 3), huespedes.getValue());
    }
}
