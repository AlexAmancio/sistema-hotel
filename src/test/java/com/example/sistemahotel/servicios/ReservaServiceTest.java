package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Reserva;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IReservaRepository;

// Cubre el riesgo real identificado en el registro de riesgos del proyecto:
// que el solapamiento de fechas se valide por cada habitación de una reserva
// múltiple, no solo por la primera de la lista.
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private IReservaRepository repositorio;

    @Mock
    private IHabitacionService habitacionService;

    @Mock
    private IRegistroService registroService;

    private ReservaService service;

    private Cliente cliente;
    private Usuario usuario;
    private Date checkIn;
    private Habitacion habitacion101;
    private Habitacion habitacion102;

    @BeforeEach
    void setUp() {
        service = new ReservaService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
        ReflectionTestUtils.setField(service, "habitacionService", habitacionService);
        ReflectionTestUtils.setField(service, "registroService", registroService);

        cliente = new Cliente();
        cliente.setId(1);

        usuario = new Usuario();
        usuario.setId(1);

        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.NOVEMBER, 10, 13, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        checkIn = cal.getTime();

        Categoria categoria = new Categoria();
        categoria.setPrecio(150.0);

        habitacion101 = new Habitacion();
        habitacion101.setId(101);
        habitacion101.setNumero("101");
        habitacion101.setAforo(2);
        habitacion101.setCategoria(categoria);

        habitacion102 = new Habitacion();
        habitacion102.setId(102);
        habitacion102.setNumero("102");
        habitacion102.setAforo(2);
        habitacion102.setCategoria(categoria);
    }

    @Test
    void creaUnRegistroPorCadaHabitacionCuandoTodoEsValido() {
        when(repositorio.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(1);
            return r;
        });
        when(habitacionService.consultarId(101)).thenReturn(Optional.of(habitacion101));
        when(habitacionService.consultarId(102)).thenReturn(Optional.of(habitacion102));
        when(registroService.existeSolapamiento(any(), any(), anyInt(), any())).thenReturn(false);

        Map<Integer, Integer> huespedes = new HashMap<>();
        huespedes.put(101, 2);
        huespedes.put(102, 1);

        Reserva reserva = service.crear(cliente, usuario, checkIn, 3, huespedes, List.of(101, 102));

        assertEquals(1, reserva.getId());
        verify(registroService, times(2)).guardar(any());
        verify(habitacionService, times(2)).guardar(any());
    }

    @Test
    void noCreaNadaSiNingunaHabitacionFueSeleccionada() {
        assertThrows(IllegalArgumentException.class,
            () -> service.crear(cliente, usuario, checkIn, 3, new HashMap<>(), List.of()));

        verify(registroService, never()).guardar(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaLaReservaSiLosHuespedesSuperanElAforo() {
        when(habitacionService.consultarId(101)).thenReturn(Optional.of(habitacion101));

        Map<Integer, Integer> huespedes = new HashMap<>();
        huespedes.put(101, 5); // el aforo de la habitación es 2

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.crear(cliente, usuario, checkIn, 3, huespedes, List.of(101)));

        assertTrue(ex.getMessage().contains("101"));
        verify(registroService, never()).guardar(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaLaReservaSiLaSegundaHabitacionTieneSolapamiento() {
        // La primera habitación está libre; la segunda es la que se cruza con
        // otra reserva activa. Si el servicio solo validara la primera de la
        // lista, este caso pasaría de largo y se crearía una reserva sobre
        // una habitación ya ocupada en esas fechas.
        when(habitacionService.consultarId(101)).thenReturn(Optional.of(habitacion101));
        when(habitacionService.consultarId(102)).thenReturn(Optional.of(habitacion102));
        when(registroService.existeSolapamiento(eq(habitacion101), any(), anyInt(), any())).thenReturn(false);
        when(registroService.existeSolapamiento(eq(habitacion102), any(), anyInt(), any())).thenReturn(true);

        Map<Integer, Integer> huespedes = new HashMap<>();
        huespedes.put(101, 1);
        huespedes.put(102, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.crear(cliente, usuario, checkIn, 3, huespedes, List.of(101, 102)));

        assertTrue(ex.getMessage().contains("102"));
        verify(registroService, never()).guardar(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaLaReservaSiUnaHabitacionYaNoExiste() {
        when(habitacionService.consultarId(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.crear(cliente, usuario, checkIn, 3, new HashMap<>(), List.of(999)));

        verify(registroService, never()).guardar(any());
    }
}
