package com.example.sistemahotel.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IComprobanteService;
import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.servicios.UsuarioService;

// Cubre el bug real encontrado en sesion: /no-show/{id}/ marcaba el registro
// como no-show sin revisar si ya tenia consumos cargados, lo cual es una
// contradiccion directa (un consumo prueba que el huesped si llego).
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "elon", roles = "ADMIN")
class RegistrosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IRegistroService service;

    @MockBean
    private IClienteService clienteService;

    @MockBean
    private IHabitacionService habitacionService;

    // SecurityConfig necesita el bean concreto UsuarioService (no la interfaz)
    // para su propio constructor, así que se mockea por esa clase.
    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private IComprobanteService comprobanteService;

    @MockBean
    private IConsumoService consumoService;

    @Test
    void rechazaNoShowSiElRegistroYaTieneConsumosCargados() throws Exception {
        when(consumoService.listarPorRegistro(6)).thenReturn(List.of(new Consumo()));

        mockMvc.perform(post("/no-show/6/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error",
                "No se puede marcar como no-show: ya tiene consumos cargados, así que sí llegó a usar la habitación."));

        verify(service, never()).marcarNoShow(anyInt());
    }

    @Test
    void marcaNoShowCuandoNoHayConsumosCargados() throws Exception {
        when(consumoService.listarPorRegistro(6)).thenReturn(List.of());

        mockMvc.perform(post("/no-show/6/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        verify(service).marcarNoShow(6);
    }

    // Mismo criterio que no-show: cancelar un registro que ya tiene consumos
    // cargados dejaría "Cancelada" (afuera de ingresos/ocupación en reportes)
    // sobre una estadía que sí pasó de verdad.
    @Test
    void rechazaCancelarSiElRegistroYaTieneConsumosCargados() throws Exception {
        when(consumoService.listarPorRegistro(6)).thenReturn(List.of(new Consumo()));

        mockMvc.perform(post("/cancelar/6/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"))
            .andExpect(flash().attribute("error",
                "No se puede cancelar: ya tiene consumos cargados, así que sí llegó a usar el servicio. Hacé check-out en vez de cancelar."));

        verify(service, never()).cancelar(anyInt());
    }

    @Test
    void cancelaCuandoNoHayConsumosCargados() throws Exception {
        when(consumoService.listarPorRegistro(6)).thenReturn(List.of());

        mockMvc.perform(post("/cancelar/6/").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        verify(service).cancelar(6);
    }

    // Bug real encontrado en sesion: /guardar/ revalidaba el aforo en CADA
    // guardado, incluso cuando ni la habitacion ni el numero de huespedes
    // cambiaban. Un registro viejo cuya habitacion le redujeron el aforo
    // despues (desde Habitaciones) quedaba bloqueado para siempre, incluso
    // para guardar algo que no toca ninguno de los dos campos, como el pago.
    // Usa datos reales de la DB de pruebas: habitacion id=15 (numero 115)
    // tiene aforo=3.
    @Test
    void permiteGuardarEdicionSinTocarHabitacionNiHuespedesAunqueYaSuperaElAforo() throws Exception {
        Habitacion habitacionExistente = new Habitacion();
        habitacionExistente.setId(15);
        Registro existente = new Registro();
        existente.setId(29);
        existente.setNumHuespedes(4);
        existente.setHabitacion(habitacionExistente);
        when(service.consultarId(29)).thenReturn(Optional.of(existente));
        when(service.existeSolapamiento(any(), any(), anyInt(), any())).thenReturn(false);
        when(service.listar()).thenReturn(List.of());

        mockMvc.perform(post("/guardar/").with(csrf())
                .param("id", "29")
                .param("cliente", "1")
                .param("habitacion", "15")
                .param("numHuespedes", "4")
                .param("checkIn", "2026-10-19T09:00")
                .param("noches", "5")
                .param("tarifa", "1100")
                .param("montoPagado", "1100")
                .param("estado", "ACTIVA"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        verify(service).guardar(any());
    }

    @Test
    void rechazaGuardarSiAumentaHuespedesPorEncimaDelAforoEnEstaEdicion() throws Exception {
        Habitacion habitacionExistente = new Habitacion();
        habitacionExistente.setId(15);
        Registro existente = new Registro();
        existente.setId(29);
        existente.setNumHuespedes(3);
        existente.setHabitacion(habitacionExistente);
        when(service.consultarId(29)).thenReturn(Optional.of(existente));

        mockMvc.perform(post("/guardar/").with(csrf())
                .param("id", "29")
                .param("cliente", "1")
                .param("habitacion", "15")
                .param("numHuespedes", "4")
                .param("checkIn", "2026-10-19T09:00")
                .param("noches", "5")
                .param("tarifa", "1100")
                .param("montoPagado", "1100")
                .param("estado", "ACTIVA"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/editar/29/"));

        verify(service, never()).guardar(any());
    }

    // Bug real encontrado en sesion: no habia forma de buscar por cliente,
    // DNI o habitacion dentro de la propia lista de registros — el unico
    // buscador era el del sidebar, que te sacaba a una pantalla distinta y
    // perdia el filtro de estado.
    @Test
    void listarFiltraPorNombreDeCliente() throws Exception {
        Registro conMilagros = new Registro();
        conMilagros.setId(1);
        Cliente milagros = new Cliente();
        milagros.setNombres("Milagros");
        milagros.setApellidos("Martínez Gutiérrez");
        conMilagros.setCliente(milagros);
        conMilagros.setHabitacion(new Habitacion());
        conMilagros.setCheckIn(new Date());
        conMilagros.setNoches(1);

        Registro conOtro = new Registro();
        conOtro.setId(2);
        Cliente otro = new Cliente();
        otro.setNombres("Carlos");
        otro.setApellidos("Quispe");
        conOtro.setCliente(otro);
        conOtro.setHabitacion(new Habitacion());
        conOtro.setCheckIn(new Date());
        conOtro.setNoches(1);

        when(service.listarOrdenadoPorPrioridad(null)).thenReturn(List.of(conMilagros, conOtro));

        mockMvc.perform(get("/").param("q", "milagros"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("registros", List.of(conMilagros)));
    }

    @Test
    void listarFiltraPorNumeroDeHabitacion() throws Exception {
        Registro enHab210 = new Registro();
        enHab210.setId(1);
        Cliente cliente1 = new Cliente();
        cliente1.setNombres("Milagros");
        cliente1.setApellidos("Martínez");
        enHab210.setCliente(cliente1);
        Habitacion hab210 = new Habitacion();
        hab210.setNumero("210");
        enHab210.setHabitacion(hab210);
        enHab210.setCheckIn(new Date());
        enHab210.setNoches(1);

        Registro enHab115 = new Registro();
        enHab115.setId(2);
        Cliente cliente2 = new Cliente();
        cliente2.setNombres("Carlos");
        cliente2.setApellidos("Quispe");
        enHab115.setCliente(cliente2);
        Habitacion hab115 = new Habitacion();
        hab115.setNumero("115");
        enHab115.setHabitacion(hab115);
        enHab115.setCheckIn(new Date());
        enHab115.setNoches(1);

        when(service.listarOrdenadoPorPrioridad(null)).thenReturn(List.of(enHab210, enHab115));

        mockMvc.perform(get("/").param("q", "115"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("registros", List.of(enHab115)));
    }
}
