package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IComprobanteService;
import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

// Casos con datos incompletos que el formulario normalmente evita (registros
// sin cliente o sin habitación, campos vacíos, ediciones de registros cuyo
// original ya no existe) y combinaciones de estado al guardar.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrosControllerRamasTest {

    @Mock private IRegistroService service;
    @Mock private IClienteService clienteService;
    @Mock private IHabitacionService habitacionService;
    @Mock private IUsuarioService usuarioService;
    @Mock private IComprobanteService comprobanteService;
    @Mock private IConsumoService consumoService;

    @InjectMocks private RegistrosController controller;

    @BeforeEach
    void iniciarSesion() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("elon", "x"));
        when(usuarioService.consultarpornombreusuario(anyString())).thenReturn(Optional.empty());
        when(comprobanteService.consultarPorRegistro(anyInt())).thenReturn(Optional.empty());
    }

    @AfterEach
    void cerrarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static Habitacion habitacion(int id, String numero) {
        Categoria categoria = new Categoria();
        categoria.setPrecio(100);
        Habitacion h = new Habitacion();
        h.setId(id);
        h.setNumero(numero);
        h.setAforo(3);
        h.setCategoria(categoria);
        return h;
    }

    private static Cliente cliente(int id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombres("Ana");
        c.setApellidos("Rojas");
        return c;
    }

    private static Date manana() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    private static Registro registro(int id, Cliente cliente, Habitacion habitacion) {
        Registro r = new Registro();
        r.setId(id);
        r.setCliente(cliente);
        r.setHabitacion(habitacion);
        r.setCheckIn(manana());
        r.setNoches(2);
        r.setNumHuespedes(2);
        r.setTarifa(200.0);
        return r;
    }

    @Test
    void unEstadoVacioNoFiltraYLaBusquedaIgnoraRegistrosSinClienteNiHabitacion() {
        Registro sinDatos = registro(1, null, null);
        when(service.listarOrdenadoPorPrioridad(null)).thenReturn(List.of(sinDatos));
        ExtendedModelMap model = new ExtendedModelMap();

        controller.listar("", "ana", 0, model);

        assertEquals(List.of(), model.get("registros"));
    }

    @Test
    void elCsvDejaVaciaLaHabitacionCuandoNoHayONoTieneNumero() throws Exception {
        Registro sinHabitacion = registro(1, cliente(1), null);
        Registro habitacionSinNumero = registro(2, cliente(2), habitacion(5, null));
        when(service.listarOrdenadoPorPrioridad(null)).thenReturn(List.of(sinHabitacion, habitacionSinNumero));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.exportar(null, null, response);

        String csv = response.getContentAsString();
        assertTrue(csv.contains("\"Ana Rojas\",\"\","));
        assertEquals(3, csv.trim().split("\n").length);
    }

    @Test
    void nuevoConFechaVaciaNoPrecargaFechaYNoAvisaPorReservasSinClienteOHabitacion() {
        Registro activoSinCliente = registro(7, null, habitacion(1, "101"));
        Registro activoSinHabitacion = registro(8, cliente(3), null);
        when(service.listar()).thenReturn(List.of(activoSinCliente, activoSinHabitacion));
        ExtendedModelMap model = new ExtendedModelMap();

        controller.nuevo(null, "", model);

        assertEquals(null, ((Registro) model.get("registro")).getCheckIn());
        assertEquals(Map.of(), model.get("clientesConReservaActiva"));
    }

    @Test
    void guardaUnCheckInNuevoSinMontoPagado() {
        Registro nuevo = registro(0, cliente(1), habitacion(1, "101"));
        nuevo.setMontoPagado(null);
        when(service.existeSolapamiento(any(), any(), anyInt(), any())).thenReturn(false);

        String vista = controller.guardar(nuevo, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertEquals("redirect:/", vista);
        verify(service).guardar(nuevo);
    }

    @Test
    void guardaUnCheckInNuevoCanceladoSinOcuparLaHabitacion() {
        Registro nuevo = registro(0, cliente(1), habitacion(1, "101"));
        nuevo.setEstado(EstadoRegistro.CANCELADA);

        controller.guardar(nuevo, new ExtendedModelMap(), new RedirectAttributesModelMap());

        verify(habitacionService, never()).guardar(any());
    }

    @Test
    void editarUnRegistroQueYaNoExisteLoGuardaComoSiFueraNuevoParaElAforo() {
        when(service.consultarId(50)).thenReturn(Optional.empty());
        Registro editado = registro(50, cliente(1), habitacion(1, "101"));

        String vista = controller.guardar(editado, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertEquals("redirect:/", vista);
        verify(service).guardar(editado);
    }

    @Test
    void editarUnRegistroCuyoOriginalNoTeniaHabitacionRecalculaLaTarifa() {
        Registro original = registro(51, cliente(1), null);
        when(service.consultarId(51)).thenReturn(Optional.of(original));
        Registro editado = registro(51, cliente(1), habitacion(1, "101"));

        controller.guardar(editado, new ExtendedModelMap(), new RedirectAttributesModelMap());

        assertEquals(200.0, editado.getTarifa());
        verify(service).guardar(editado);
    }

    @Test
    void cambiarDeHabitacionUnRegistroFinalizadoNoOcupaLaNuevaYLiberaLaAnterior() {
        Registro original = registro(52, cliente(1), habitacion(1, "101"));
        when(service.consultarId(52)).thenReturn(Optional.of(original));
        Registro editado = registro(52, cliente(1), habitacion(2, "102"));
        editado.setEstado(EstadoRegistro.FINALIZADA);
        Registro otroSinHabitacion = registro(53, cliente(2), null);
        when(service.listar()).thenReturn(List.of(otroSinHabitacion));
        Habitacion anterior = habitacion(1, "101");
        when(habitacionService.consultarId(1)).thenReturn(Optional.of(anterior));

        controller.guardar(editado, new ExtendedModelMap(), new RedirectAttributesModelMap());

        verify(habitacionService).guardar(anterior);
        verify(habitacionService, never()).guardar(editado.getHabitacion());
    }

    @Test
    void laListaMuestraElComprobanteDeLosRegistrosQueYaLoTienen() {
        Registro conComprobante = registro(1, cliente(1), habitacion(1, "101"));
        when(service.listarOrdenadoPorPrioridad(null)).thenReturn(List.of(conComprobante));
        Comprobante comprobante = new Comprobante();
        when(comprobanteService.consultarPorRegistro(1)).thenReturn(Optional.of(comprobante));
        ExtendedModelMap model = new ExtendedModelMap();

        controller.listar(null, null, 0, model);

        assertEquals(Map.of(1, comprobante), model.get("comprobantes"));
    }
}
