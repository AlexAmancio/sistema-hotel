package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Reserva;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IReservaRepository;

// Detalle de la reserva de grupo (HU47): tarifa por temporada (RN04), habitación
// ocupada (RN24), huéspedes por defecto (RN30) y usuario responsable (RN42).
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class ReservaServiceDetalleTest {

    @Mock
    private IReservaRepository repositorio;

    @Mock
    private IHabitacionService habitacionService;

    @Mock
    private IRegistroService registroService;

    private ReservaService service;
    private Cliente cliente;
    private Usuario usuario;
    private Habitacion habitacion;
    private Date checkIn;

    @BeforeEach
    void setUp() {
        service = new ReservaService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
        ReflectionTestUtils.setField(service, "habitacionService", habitacionService);
        ReflectionTestUtils.setField(service, "registroService", registroService);

        cliente = new Cliente();
        cliente.setId(1);
        usuario = new Usuario();
        usuario.setId(2);
        usuario.setUsername("mquispe");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2026, Calendar.DECEMBER, 26, 14, 0, 0);
        checkIn = cal.getTime();

        Categoria categoria = new Categoria();
        categoria.setPrecio(260.0);
        categoria.setPrecioTemporadaAlta(320.0);
        Calendar ini = Calendar.getInstance(); ini.clear(); ini.set(2026, Calendar.DECEMBER, 20);
        Calendar fin = Calendar.getInstance(); fin.clear(); fin.set(2027, Calendar.JANUARY, 5);
        categoria.setTemporadaAltaInicio(ini.getTime());
        categoria.setTemporadaAltaFin(fin.getTime());

        habitacion = new Habitacion();
        habitacion.setId(22);
        habitacion.setNumero("201");
        habitacion.setAforo(4);
        habitacion.setCategoria(categoria);

        when(habitacionService.consultarId(22)).thenReturn(Optional.of(habitacion));
        when(registroService.existeSolapamiento(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any())).thenReturn(false);
        when(repositorio.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Registro registroGuardado() {
        ArgumentCaptor<Registro> captor = ArgumentCaptor.forClass(Registro.class);
        verify(registroService).guardar(captor.capture());
        return captor.getValue();
    }

    @Test
    void enTemporadaAltaLaTarifaUsaElPrecioAltoPorLasNoches() {
        service.crear(cliente, usuario, checkIn, 3, Map.of(22, 2), List.of(22));

        assertEquals(960.0, registroGuardado().getTarifa());
    }

    @Test
    void laHabitacionReservadaQuedaOcupada() {
        service.crear(cliente, usuario, checkIn, 3, Map.of(22, 2), List.of(22));

        assertEquals(EstadoHabitacion.OCUPADA, habitacion.getEstado());
        verify(habitacionService).guardar(habitacion);
    }

    @Test
    void siNoSeIndicanHuespedesSeAsumeUno() {
        service.crear(cliente, usuario, checkIn, 3, Map.of(), List.of(22));

        assertEquals(1, registroGuardado().getNumHuespedes());
    }

    @Test
    void cadaEstadiaQuedaANombreDelClienteYDelUsuarioEnSesion() {
        service.crear(cliente, usuario, checkIn, 3, Map.of(22, 2), List.of(22));

        Registro registro = registroGuardado();
        assertSame(cliente, registro.getCliente());
        assertSame(usuario, registro.getUsuario());
        assertEquals(3, registro.getNoches());
        assertSame(checkIn, registro.getCheckIn());
    }

    @Test
    void laReservaGuardadaGuardaClienteYUsuario() {
        Reserva reserva = service.crear(cliente, usuario, checkIn, 3, Map.of(22, 2), List.of(22));

        assertSame(cliente, reserva.getCliente());
        assertSame(usuario, reserva.getUsuario());
    }
}
