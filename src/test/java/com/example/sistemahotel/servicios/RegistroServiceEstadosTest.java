package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IHabitacionRepository;
import com.example.sistemahotel.repositories.IRegistroRepository;

// Ciclo de vida de una estadía y estado de la habitación (reglas RN01, RN06,
// RN17 y RN44): check-out, cancelación, no-show, cruce de fechas y orden de la
// lista de registros.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class RegistroServiceEstadosTest {

    @Mock
    private IRegistroRepository repositorio;

    @Mock
    private IHabitacionRepository habitacionrepositorio;

    private RegistroService service;

    private Habitacion habitacion;

    @BeforeEach
    void setUp() {
        service = new RegistroService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
        ReflectionTestUtils.setField(service, "habitacionrepositorio", habitacionrepositorio);
        habitacion = new Habitacion();
        habitacion.setId(1);
        habitacion.setNumero("101");
        habitacion.setEstado(EstadoHabitacion.OCUPADA);
    }

    private Date diasDesdeHoy(int dias, int hora) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, dias);
        cal.set(Calendar.HOUR_OF_DAY, hora);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Registro registro(int id, Habitacion hab, Date checkIn, int noches, EstadoRegistro estado) {
        Registro registro = new Registro();
        registro.setId(id);
        registro.setHabitacion(hab);
        registro.setCheckIn(checkIn);
        registro.setNoches(noches);
        registro.setEstado(estado);
        return registro;
    }

    @Test
    void checkoutFinalizaLaEstadiaGuardaLaHoraRealYMandaLaHabitacionALimpieza() {
        Registro activo = registro(6, habitacion, diasDesdeHoy(-2, 12), 2, EstadoRegistro.ACTIVA);
        when(repositorio.findById(6)).thenReturn(Optional.of(activo));

        service.hacerCheckout(6);

        assertEquals(EstadoRegistro.FINALIZADA, activo.getEstado());
        assertNotNull(activo.getCheckOutReal());
        assertEquals(EstadoHabitacion.LIMPIEZA, habitacion.getEstado());
        verify(repositorio).save(activo);
        verify(habitacionrepositorio).save(habitacion);
    }

    @Test
    void checkoutDeUnRegistroInexistenteNoGuardaNada() {
        when(repositorio.findById(99)).thenReturn(Optional.empty());

        service.hacerCheckout(99);

        verify(repositorio, never()).save(any());
        verify(habitacionrepositorio, never()).save(any());
    }

    @Test
    void cancelarDejaLaEstadiaCanceladaYLaHabitacionOcupadaPasaALimpieza() {
        Registro activo = registro(7, habitacion, diasDesdeHoy(0, 12), 1, EstadoRegistro.ACTIVA);
        when(repositorio.findById(7)).thenReturn(Optional.of(activo));

        service.cancelar(7);

        assertEquals(EstadoRegistro.CANCELADA, activo.getEstado());
        assertEquals(EstadoHabitacion.LIMPIEZA, habitacion.getEstado());
    }

    @Test
    void noShowDejaLaEstadiaComoNoShowYLaHabitacionOcupadaVuelveADisponible() {
        Registro activo = registro(8, habitacion, diasDesdeHoy(0, 12), 1, EstadoRegistro.ACTIVA);
        when(repositorio.findById(8)).thenReturn(Optional.of(activo));

        service.marcarNoShow(8);

        assertEquals(EstadoRegistro.NO_SHOW, activo.getEstado());
        assertEquals(EstadoHabitacion.DISPONIBLE, habitacion.getEstado());
    }

    @Test
    void cancelarNoCambiaUnaHabitacionQueNoEstabaOcupada() {
        habitacion.setEstado(EstadoHabitacion.MANTENIMIENTO);
        Registro activo = registro(9, habitacion, diasDesdeHoy(3, 12), 1, EstadoRegistro.ACTIVA);
        when(repositorio.findById(9)).thenReturn(Optional.of(activo));

        service.cancelar(9);

        assertEquals(EstadoHabitacion.MANTENIMIENTO, habitacion.getEstado());
        verify(habitacionrepositorio, never()).save(any());
    }

    @Test
    void hayCruceSiLaNuevaEstadiaEmpiezaAntesDeQueTermineOtraActiva() {
        Registro existente = registro(1, habitacion, diasDesdeHoy(10, 12), 3, EstadoRegistro.ACTIVA);
        when(repositorio.findAll()).thenReturn(List.of(existente));

        assertTrue(service.existeSolapamiento(habitacion, diasDesdeHoy(11, 12), 2, null));
    }

    @Test
    void noHayCruceSiLaNuevaEstadiaEmpiezaJustoCuandoTerminaLaOtra() {
        Registro existente = registro(1, habitacion, diasDesdeHoy(10, 12), 3, EstadoRegistro.ACTIVA);
        when(repositorio.findAll()).thenReturn(List.of(existente));

        assertFalse(service.existeSolapamiento(habitacion, diasDesdeHoy(13, 12), 2, null));
    }

    @Test
    void noHayCruceConLaMismaEstadiaQueSeEstaEditando() {
        Registro existente = registro(1, habitacion, diasDesdeHoy(10, 12), 3, EstadoRegistro.ACTIVA);
        when(repositorio.findAll()).thenReturn(List.of(existente));

        assertFalse(service.existeSolapamiento(habitacion, diasDesdeHoy(10, 12), 5, 1));
    }

    @Test
    void lasEstadiasCanceladasNoBloqueanLaHabitacion() {
        Registro cancelada = registro(1, habitacion, diasDesdeHoy(10, 12), 3, EstadoRegistro.CANCELADA);
        when(repositorio.findAll()).thenReturn(List.of(cancelada));

        assertFalse(service.existeSolapamiento(habitacion, diasDesdeHoy(11, 12), 1, null));
    }

    @Test
    void unaEstadiaDeOtraHabitacionNoGeneraCruce() {
        Habitacion otra = new Habitacion();
        otra.setId(2);
        Registro existente = registro(1, otra, diasDesdeHoy(10, 12), 3, EstadoRegistro.ACTIVA);
        when(repositorio.findAll()).thenReturn(List.of(existente));

        assertFalse(service.existeSolapamiento(habitacion, diasDesdeHoy(11, 12), 1, null));
    }

    @Test
    void extenderUnRegistroInexistenteDevuelveFalso() {
        when(repositorio.findById(50)).thenReturn(Optional.empty());

        assertFalse(service.extenderEstadia(50, 4));
    }

    @Test
    void laListaPoneVencidasPrimeroLuegoActivasYAlFinalElHistorial() {
        Registro finalizada = registro(1, habitacion, diasDesdeHoy(-20, 12), 2, EstadoRegistro.FINALIZADA);
        Registro activaAlDia = registro(2, habitacion, diasDesdeHoy(1, 12), 2, EstadoRegistro.ACTIVA);
        Registro vencida = registro(3, habitacion, diasDesdeHoy(-10, 12), 2, EstadoRegistro.ACTIVA);
        when(repositorio.findAll()).thenReturn(List.of(finalizada, activaAlDia, vencida));

        List<Registro> orden = service.listarOrdenadoPorPrioridad(null);

        assertEquals(List.of(3, 2, 1), orden.stream().map(Registro::getId).toList());
    }

    @Test
    void entreActivasPrimeroVaLaQueSaleAntes() {
        Registro saleLuego = registro(1, habitacion, diasDesdeHoy(1, 12), 5, EstadoRegistro.ACTIVA);
        Registro saleAntes = registro(2, habitacion, diasDesdeHoy(1, 12), 1, EstadoRegistro.ACTIVA);
        when(repositorio.findAll()).thenReturn(List.of(saleLuego, saleAntes));

        List<Registro> orden = service.listarOrdenadoPorPrioridad(null);

        assertEquals(List.of(2, 1), orden.stream().map(Registro::getId).toList());
    }

    @Test
    void laListaSePuedeFiltrarPorEstado() {
        Registro finalizada = registro(1, habitacion, diasDesdeHoy(-20, 12), 2, EstadoRegistro.FINALIZADA);
        Registro activa = registro(2, habitacion, diasDesdeHoy(1, 12), 2, EstadoRegistro.ACTIVA);
        when(repositorio.findAll()).thenReturn(List.of(finalizada, activa));

        List<Registro> soloFinalizadas = service.listarOrdenadoPorPrioridad(EstadoRegistro.FINALIZADA);

        assertEquals(List.of(1), soloFinalizadas.stream().map(Registro::getId).toList());
    }

    @Test
    void buscarPorNumeroDeHabitacionDelegaEnElRepositorio() {
        Registro r = registro(1, habitacion, diasDesdeHoy(1, 12), 2, EstadoRegistro.ACTIVA);
        when(repositorio.findByHabitacion_NumeroContainingIgnoreCase("10")).thenReturn(List.of(r));

        assertEquals(1, service.buscarPorNumeroHabitacion("10").size());
    }
}
