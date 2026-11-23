package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IHabitacionRepository;
import com.example.sistemahotel.repositories.IRegistroRepository;

// Casos límite de RegistroService que no cubrían las demás pruebas: registros
// sin habitación (datos antiguos), habitaciones en mantenimiento, estadías
// contiguas y el registro que se está editando (RN01, RN04, RN06 y RN18).
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class RegistroServiceBordesTest {

    @Mock
    private IRegistroRepository repositorio;

    @Mock
    private IHabitacionRepository habitacionrepositorio;

    private RegistroService service;

    @BeforeEach
    void setUp() {
        service = new RegistroService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
        ReflectionTestUtils.setField(service, "habitacionrepositorio", habitacionrepositorio);
    }

    private Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes - 1, dia, 12, 0);
        return cal.getTime();
    }

    private Habitacion habitacion(int id, EstadoHabitacion estado) {
        Categoria categoria = new Categoria();
        categoria.setPrecio(100.0);
        Habitacion h = new Habitacion();
        h.setId(id);
        h.setNumero("H" + id);
        h.setEstado(estado);
        h.setCategoria(categoria);
        return h;
    }

    private Registro registro(int id, Habitacion habitacion, Date checkIn, int noches, EstadoRegistro estado) {
        Registro r = new Registro();
        r.setId(id);
        r.setHabitacion(habitacion);
        r.setCheckIn(checkIn);
        r.setNoches(noches);
        r.setEstado(estado);
        return r;
    }

    @Test
    void eliminarUnaEstadiaActivaCuyaHabitacionYaEstaEnLimpiezaNoLaCambia() {
        Habitacion h = habitacion(1, EstadoHabitacion.LIMPIEZA);
        when(repositorio.findById(5)).thenReturn(Optional.of(registro(5, h, new Date(), 1, EstadoRegistro.ACTIVA)));

        service.eliminar(5);

        verify(habitacionrepositorio, never()).save(any());
        verify(repositorio).deleteById(5);
        assertEquals(EstadoHabitacion.LIMPIEZA, h.getEstado());
    }

    @Test
    void eliminarUnaEstadiaSinHabitacionLaBorraSinTocarHabitaciones() {
        when(repositorio.findById(6)).thenReturn(Optional.of(registro(6, null, new Date(), 1, EstadoRegistro.ACTIVA)));

        service.eliminar(6);

        verify(habitacionrepositorio, never()).save(any());
        verify(repositorio).deleteById(6);
    }

    @Test
    void eliminarUnRegistroQueYaNoExisteIgualPideBorrarlo() {
        when(repositorio.findById(7)).thenReturn(Optional.empty());

        service.eliminar(7);

        verify(repositorio).deleteById(7);
    }

    @Test
    void lasHabitacionesEnMantenimientoNoSeOfrecen() {
        Habitacion libre = habitacion(1, EstadoHabitacion.DISPONIBLE);
        Habitacion mantenimiento = habitacion(2, EstadoHabitacion.MANTENIMIENTO);
        when(habitacionrepositorio.findAll()).thenReturn(List.of(libre, mantenimiento));
        when(repositorio.findAll()).thenReturn(List.of());

        List<Habitacion> disponibles = service.obtenerHabitacionesDisponibles(fecha(2028, 3, 1), 2, null);

        assertEquals(List.of(libre), disponibles);
    }

    @Test
    void alEditarLaPropiaEstadiaNoBloqueaSuHabitacion() {
        Habitacion h = habitacion(1, EstadoHabitacion.OCUPADA);
        Registro propio = registro(9, h, fecha(2028, 3, 1), 3, EstadoRegistro.ACTIVA);
        when(habitacionrepositorio.findAll()).thenReturn(List.of(h));
        when(repositorio.findAll()).thenReturn(List.of(propio));
        when(repositorio.findById(9)).thenReturn(Optional.of(propio));

        assertEquals(List.of(h), service.obtenerHabitacionesDisponibles(fecha(2028, 3, 2), 1, 9));
    }

    @Test
    void alEditarUnaEstadiaSinHabitacionSeOfrecenLasLibres() {
        Habitacion libre = habitacion(1, EstadoHabitacion.DISPONIBLE);
        Habitacion limpieza = habitacion(2, EstadoHabitacion.LIMPIEZA);
        when(habitacionrepositorio.findAll()).thenReturn(List.of(libre, limpieza));
        when(repositorio.findAll()).thenReturn(List.of());
        when(repositorio.findById(10)).thenReturn(Optional.of(registro(10, null, fecha(2028, 3, 1), 1, EstadoRegistro.ACTIVA)));

        assertEquals(List.of(libre), service.obtenerHabitacionesDisponibles(fecha(2028, 3, 1), 1, 10));
    }

    @Test
    void unaEstadiaQueTerminaJustoCuandoEmpiezaOtraNoSeCruza() {
        Habitacion h = habitacion(1, EstadoHabitacion.OCUPADA);
        when(repositorio.findAll()).thenReturn(List.of(registro(1, h, fecha(2028, 3, 5), 2, EstadoRegistro.ACTIVA)));

        // Nueva: 3 al 5 de marzo; la existente empieza el 5.
        assertFalse(service.existeSolapamiento(h, fecha(2028, 3, 3), 2, null));
        // Nueva: 4 al 6 de marzo; sí se cruza.
        assertTrue(service.existeSolapamiento(h, fecha(2028, 3, 4), 2, null));
    }

    @Test
    void lasEstadiasActivasSinHabitacionNoGeneranCruce() {
        Habitacion h = habitacion(1, EstadoHabitacion.DISPONIBLE);
        when(repositorio.findAll()).thenReturn(List.of(registro(1, null, fecha(2028, 3, 1), 5, EstadoRegistro.ACTIVA)));

        assertFalse(service.existeSolapamiento(h, fecha(2028, 3, 2), 1, null));
    }

    @Test
    void checkoutDeUnaEstadiaSinHabitacionSoloLaFinaliza() {
        Registro r = registro(3, null, new Date(), 1, EstadoRegistro.ACTIVA);
        when(repositorio.findById(3)).thenReturn(Optional.of(r));

        service.hacerCheckout(3);

        assertEquals(EstadoRegistro.FINALIZADA, r.getEstado());
        verify(habitacionrepositorio, never()).save(any());
    }

    @Test
    void cancelarUnaEstadiaSinHabitacionSoloCambiaSuEstado() {
        Registro r = registro(4, null, new Date(), 1, EstadoRegistro.ACTIVA);
        when(repositorio.findById(4)).thenReturn(Optional.of(r));

        service.cancelar(4);

        assertEquals(EstadoRegistro.CANCELADA, r.getEstado());
        verify(habitacionrepositorio, never()).save(any());
    }

    @Test
    void noSePuedeExtenderUnaEstadiaSinHabitacion() {
        Registro r = registro(8, null, new Date(), 1, EstadoRegistro.ACTIVA);
        when(repositorio.findById(8)).thenReturn(Optional.of(r));

        assertFalse(service.extenderEstadia(8, 3));
        assertEquals(1, r.getNoches());
        verify(repositorio, never()).save(any());
    }

    @Test
    void lasOcupadasAhoraIgnoranRegistrosSinHabitacionOSinFecha() {
        Habitacion h = habitacion(1, EstadoHabitacion.OCUPADA);
        Date ayer = new Date(System.currentTimeMillis() - 86_400_000L);
        when(repositorio.findAll()).thenReturn(List.of(
            registro(1, h, ayer, 3, EstadoRegistro.ACTIVA),
            registro(2, null, ayer, 3, EstadoRegistro.ACTIVA),
            registro(3, habitacion(2, EstadoHabitacion.OCUPADA), null, 3, EstadoRegistro.ACTIVA)));

        assertEquals(Set.of(1), service.idsHabitacionesOcupadasAhora(new Date()));
    }
}
