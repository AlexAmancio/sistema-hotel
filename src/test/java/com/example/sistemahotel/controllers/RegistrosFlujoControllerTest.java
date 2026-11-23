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

import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.test.context.support.WithMockUser;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Flujo de la estadía (HU31 a HU46) y reglas RN01, RN02, RN06, RN10, RN24 y RN42.
// El cliente y la habitación del formulario se resuelven contra la base de
// demostración (bd/respaldo_bd_actual.sql): cliente 1 y habitación 1 (101, aforo 2).
@WithMockUser(username = "elon", roles = "ADMIN")
class RegistrosFlujoControllerTest extends ControladorIntegracionBase {

    private Registro registroParaCsv() {
        Cliente cliente = new Cliente();
        cliente.setNombres("Karen");
        cliente.setApellidos("Díaz Pérez");
        Habitacion habitacion = new Habitacion();
        habitacion.setNumero("C1");
        Usuario usuario = new Usuario();
        usuario.setNombres("María");
        Registro registro = new Registro();
        registro.setCliente(cliente);
        registro.setHabitacion(habitacion);
        registro.setUsuario(usuario);
        Calendar cal = Calendar.getInstance(); cal.clear(); cal.set(2026, Calendar.OCTOBER, 24, 12, 0);
        registro.setCheckIn(cal.getTime());
        registro.setNoches(5);
        registro.setTarifa(2250.0);
        registro.setMontoPagado(1125.0);
        return registro;
    }

    @Test
    void unCheckInSinClienteSeRechaza() throws Exception {
        mockMvc.perform(post("/guardar/").with(csrf()).param("habitacion", "1").param("checkIn", "2027-03-01T12:00").param("noches", "2").param("numHuespedes", "1"))
            .andExpect(redirectedUrl("/nuevo/"))
            .andExpect(flash().attribute("error", "Elegí un cliente antes de guardar."));
        verify(registroService, never()).guardar(any());
    }

    @Test
    void unCheckInSinHabitacionSeRechaza() throws Exception {
        mockMvc.perform(post("/guardar/").with(csrf()).param("cliente", "1").param("checkIn", "2027-03-01T12:00").param("noches", "2").param("numHuespedes", "1"))
            .andExpect(redirectedUrl("/nuevo/"))
            .andExpect(flash().attribute("error", "Elegí una habitación antes de guardar."));
    }

    @Test
    void unCheckInConMasHuespedesQueElAforoSeRechaza() throws Exception {
        mockMvc.perform(post("/guardar/").with(csrf()).param("cliente", "1").param("habitacion", "1").param("checkIn", "2027-03-01T12:00")
                .param("noches", "2").param("numHuespedes", "3"))
            .andExpect(redirectedUrl("/nuevo/"))
            .andExpect(flash().attribute("error", "El número de huéspedes (3) supera el aforo máximo de la habitación 101 (2 personas)."));
        verify(registroService, never()).guardar(any());
    }

    @Test
    void unCheckInQueSeCruzaConOtraEstadiaSeRechaza() throws Exception {
        when(registroService.existeSolapamiento(any(), any(), anyInt(), any())).thenReturn(true);

        mockMvc.perform(post("/guardar/").with(csrf()).param("cliente", "1").param("habitacion", "1").param("checkIn", "2027-03-01T12:00")
                .param("noches", "2").param("numHuespedes", "2"))
            .andExpect(redirectedUrl("/nuevo/"))
            .andExpect(flash().attribute("error", "La habitación 101 ya tiene otra reserva activa que se cruza con esas fechas."));
        verify(registroService, never()).guardar(any());
    }

    @Test
    void unCheckInValidoQuedaANombreDelUsuarioYOcupaLaHabitacion() throws Exception {
        Usuario elon = new Usuario();
        elon.setUsername("elon");
        when(usuarioService.consultarpornombreusuario("elon")).thenReturn(Optional.of(elon));
        when(registroService.existeSolapamiento(any(), any(), anyInt(), any())).thenReturn(false);

        mockMvc.perform(post("/guardar/").with(csrf()).param("cliente", "1").param("habitacion", "1").param("checkIn", "2027-03-01T12:00")
                .param("noches", "2").param("numHuespedes", "2").param("tarifa", "360").param("montoPagado", "180").param("estado", "ACTIVA"))
            .andExpect(redirectedUrl("/"));

        ArgumentCaptor<Registro> registro = ArgumentCaptor.forClass(Registro.class);
        verify(registroService).guardar(registro.capture());
        org.junit.jupiter.api.Assertions.assertSame(elon, registro.getValue().getUsuario());
        ArgumentCaptor<Habitacion> habitacion = ArgumentCaptor.forClass(Habitacion.class);
        verify(habitacionService).guardar(habitacion.capture());
        org.junit.jupiter.api.Assertions.assertEquals(EstadoHabitacion.OCUPADA, habitacion.getValue().getEstado());
    }

    @Test
    void elCheckOutLlamaAlServicioYMuestraLasFinalizadas() throws Exception {
        mockMvc.perform(post("/checkout/6/").with(csrf()))
            .andExpect(redirectedUrl("/?estado=FINALIZADA"));
        verify(registroService).hacerCheckout(6);
    }

    @Test
    void noSePuedeEliminarUnaEstadiaConComprobante() throws Exception {
        when(comprobanteService.consultarPorRegistro(6)).thenReturn(Optional.of(new Comprobante()));

        mockMvc.perform(post("/eliminar/6/").with(csrf()))
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error", "No se puede eliminar: este registro ya tiene un comprobante emitido."));
        verify(registroService, never()).eliminar(anyInt());
    }

    @Test
    void noSePuedeEliminarUnaEstadiaConConsumos() throws Exception {
        when(comprobanteService.consultarPorRegistro(6)).thenReturn(Optional.empty());
        when(consumoService.listarPorRegistro(6)).thenReturn(List.of(new Consumo()));

        mockMvc.perform(post("/eliminar/6/").with(csrf()))
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error", "No se puede eliminar: este registro tiene consumos cargados."));
        verify(registroService, never()).eliminar(anyInt());
    }

    @Test
    void eliminarUnaEstadiaSinConsumosNiComprobante() throws Exception {
        when(comprobanteService.consultarPorRegistro(6)).thenReturn(Optional.empty());
        when(consumoService.listarPorRegistro(6)).thenReturn(List.of());

        mockMvc.perform(post("/eliminar/6/").with(csrf())).andExpect(redirectedUrl("/"));
        verify(registroService).eliminar(6);
    }

    private void estadiaDeDosNoches() {
        Registro registro = new Registro();
        registro.setId(6);
        registro.setNoches(2);
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro));
    }

    @Test
    void extenderConCruceDeFechasSeRechaza() throws Exception {
        estadiaDeDosNoches();
        when(registroService.extenderEstadia(6, 5)).thenReturn(false);

        mockMvc.perform(post("/extender/6/").with(csrf()).param("noches", "5"))
            .andExpect(redirectedUrl("/extender/6/"))
            .andExpect(flash().attribute("error", "No se pudo extender: la habitación ya tiene otra reserva activa que se cruzaría con las nuevas fechas."));
    }

    @Test
    void extenderSinCruceVuelveALaLista() throws Exception {
        estadiaDeDosNoches();
        when(registroService.extenderEstadia(6, 3)).thenReturn(true);

        mockMvc.perform(post("/extender/6/").with(csrf()).param("noches", "3")).andExpect(redirectedUrl("/"));
    }

    @Test
    void laExportacionCsvTraeEncabezadoYUnaFilaPorEstadia() throws Exception {
        when(registroService.listarOrdenadoPorPrioridad(any())).thenReturn(List.of(registroParaCsv()));

        mockMvc.perform(get("/exportar/"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Cliente,Habitacion,Check-in,Checkout estimado,Estado,Tarifa,Saldo pendiente,Registrado por")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Karen Díaz Pérez")));
    }

    @Test
    void laPantallaHoySeMuestra() throws Exception {
        when(registroService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/hoy/"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("llegan", "salen", "vencidas"));
    }
}
