package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

// Cubre el bug real encontrado en produccion: extenderEstadia() cambiaba las
// noches pero nunca recalculaba la tarifa, dejando la estadia cobrada al
// precio original sin importar cuanto se extendiera.
@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock
    private IRegistroRepository repositorio;

    @Mock
    private IHabitacionRepository habitacionrepositorio;

    private RegistroService service;

    private Registro registro;

    @BeforeEach
    void setUp() {
        service = new RegistroService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
        ReflectionTestUtils.setField(service, "habitacionrepositorio", habitacionrepositorio);

        Categoria categoria = new Categoria();
        categoria.setNombre("Habitación Estándar");
        categoria.setPrecio(150.0);

        Habitacion habitacion = new Habitacion();
        habitacion.setId(101);
        habitacion.setNumero("101");
        habitacion.setCategoria(categoria);

        registro = new Registro();
        registro.setId(6);
        registro.setHabitacion(habitacion);
        registro.setCheckIn(fecha(2026, Calendar.AUGUST, 27));
        registro.setNoches(10);
        registro.setTarifa(150.0);
        registro.setEstado(EstadoRegistro.ACTIVA);
    }

    private Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.set(anio, mes, dia, 13, 29, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    void extenderEstadiaRecalculaLaTarifaConLasNochesNuevas() {
        when(repositorio.findById(6)).thenReturn(Optional.of(registro));
        when(repositorio.findAll()).thenReturn(List.of()); // sin otros registros activos: no hay solapamiento

        boolean ok = service.extenderEstadia(6, 12);

        assertTrue(ok);

        ArgumentCaptor<Registro> captor = ArgumentCaptor.forClass(Registro.class);
        verify(repositorio).save(captor.capture());

        Registro guardado = captor.getValue();
        assertEquals(12, guardado.getNoches());
        assertEquals(1800.0, guardado.getTarifa(), 0.001); // 150/noche * 12 noches, no 150 plano
    }

    @Test
    void extenderEstadiaNoTocaNadaSiHaySolapamiento() {
        Registro otroRegistro = new Registro();
        otroRegistro.setId(99);
        otroRegistro.setHabitacion(registro.getHabitacion());
        otroRegistro.setCheckIn(fecha(2026, Calendar.SEPTEMBER, 1));
        otroRegistro.setNoches(3);
        otroRegistro.setEstado(EstadoRegistro.ACTIVA);

        when(repositorio.findById(6)).thenReturn(Optional.of(registro));
        when(repositorio.findAll()).thenReturn(List.of(registro, otroRegistro));

        boolean ok = service.extenderEstadia(6, 12);

        assertTrue(!ok);
        verify(repositorio, org.mockito.Mockito.never()).save(any());
    }

    // Bug real encontrado en sesion: eliminar() borraba el registro con un
    // deleteById() crudo, sin tocar la habitacion. Si el registro borrado era
    // el que tenia la habitacion Ocupada, esta quedaba huerfana en ese estado
    // para siempre, sin ningun registro detras que la explique.
    @Test
    void eliminarLiberaLaHabitacionSiEstabaOcupadaPorEsteRegistro() {
        registro.getHabitacion().setEstado(EstadoHabitacion.OCUPADA);
        when(repositorio.findById(6)).thenReturn(Optional.of(registro));

        service.eliminar(6);

        assertEquals(EstadoHabitacion.DISPONIBLE, registro.getHabitacion().getEstado());
        verify(habitacionrepositorio).save(registro.getHabitacion());
        verify(repositorio).deleteById(6);
    }

    @Test
    void eliminarNoTocaLaHabitacionSiElRegistroYaNoEstaActivo() {
        registro.setEstado(EstadoRegistro.FINALIZADA);
        registro.getHabitacion().setEstado(EstadoHabitacion.LIMPIEZA);
        when(repositorio.findById(6)).thenReturn(Optional.of(registro));

        service.eliminar(6);

        assertEquals(EstadoHabitacion.LIMPIEZA, registro.getHabitacion().getEstado());
        verify(habitacionrepositorio, org.mockito.Mockito.never()).save(any());
        verify(repositorio).deleteById(6);
    }

    // Bug real encontrado en sesion: el check-out deja la habitacion en
    // LIMPIEZA, y el filtro de "disponibles" excluia cualquier habitacion en
    // LIMPIEZA sin excepcion. Al editar ese mismo registro ya finalizado, el
    // selector "perdia" su propia habitacion — el resumen mostraba "Sin
    // habitacion elegida" y el campo oculto quedaba expuesto a guardarse
    // vacio si algo disparaba una nueva busqueda (ej. tocar las fechas).
    @Test
    void obtenerHabitacionesDisponiblesIncluyeLaHabitacionActualAunqueEsteEnLimpieza() {
        registro.getHabitacion().setEstado(EstadoHabitacion.LIMPIEZA);
        when(repositorio.findById(6)).thenReturn(Optional.of(registro));
        when(repositorio.findAll()).thenReturn(List.of());
        when(habitacionrepositorio.findAll()).thenReturn(List.of(registro.getHabitacion()));

        List<Habitacion> disponibles = service.obtenerHabitacionesDisponibles(registro.getCheckIn(), registro.getNoches(), 6);

        assertEquals(1, disponibles.size());
        assertEquals(101, disponibles.get(0).getId());
    }

    @Test
    void obtenerHabitacionesDisponiblesSigueExcluyendoOtrasHabitacionesEnLimpieza() {
        Habitacion otra = new Habitacion();
        otra.setId(202);
        otra.setEstado(EstadoHabitacion.LIMPIEZA);
        when(repositorio.findById(6)).thenReturn(Optional.of(registro));
        when(repositorio.findAll()).thenReturn(List.of());
        when(habitacionrepositorio.findAll()).thenReturn(List.of(otra));

        List<Habitacion> disponibles = service.obtenerHabitacionesDisponibles(registro.getCheckIn(), registro.getNoches(), 6);

        assertTrue(disponibles.isEmpty());
    }

    // Bug real encontrado en sesion: Reportes y Habitaciones calculaban
    // "ocupadas ahora" leyendo habitacion.estado, un campo espejo que puede
    // quedar desincronizado de los registros reales (ej. datos cargados
    // directo en la base, sin pasar por el check-in de la app). Mostraba
    // "Ocupación actual: 0%" con habitaciones realmente ocupadas.
    @Test
    void idsHabitacionesOcupadasAhoraIncluyeRegistroActivaConCheckInYaPasado() {
        when(repositorio.findAll()).thenReturn(List.of(registro));

        // registro: check-in 27/ago/2026, ACTIVA. "Ahora" es un momento
        // posterior, aunque el rango estimado de noches ya haya terminado:
        // sigue ACTIVA (no se hizo check-out real) y por eso sigue ocupando.
        Date ahora = fecha(2026, Calendar.SEPTEMBER, 20);

        assertEquals(java.util.Set.of(101), service.idsHabitacionesOcupadasAhora(ahora));
    }

    @Test
    void idsHabitacionesOcupadasAhoraIgnoraReservaFutura() {
        when(repositorio.findAll()).thenReturn(List.of(registro));

        // "Ahora" es ANTES del check-in del registro: es una reserva futura,
        // todavía no hay nadie en la habitación.
        Date ahora = fecha(2026, Calendar.AUGUST, 1);

        assertTrue(service.idsHabitacionesOcupadasAhora(ahora).isEmpty());
    }

    // Segundo bug real encontrado al verificar el fix anterior: la primera
    // versión incluía también registros FINALIZADA cuyo rango estimado
    // "tocaba" el día de hoy, contando como ocupados a huéspedes que ya
    // habían hecho check-out esa misma mañana.
    @Test
    void idsHabitacionesOcupadasAhoraIgnoraRegistroYaFinalizado() {
        registro.setEstado(EstadoRegistro.FINALIZADA);
        when(repositorio.findAll()).thenReturn(List.of(registro));

        Date ahora = fecha(2026, Calendar.SEPTEMBER, 1);

        assertTrue(service.idsHabitacionesOcupadasAhora(ahora).isEmpty());
    }

    @Test
    void idsHabitacionesOcupadasAhoraIgnoraCanceladasYNoShow() {
        registro.setEstado(EstadoRegistro.CANCELADA);
        when(repositorio.findAll()).thenReturn(List.of(registro));

        Date ahora = fecha(2026, Calendar.SEPTEMBER, 1);

        assertTrue(service.idsHabitacionesOcupadasAhora(ahora).isEmpty());
    }
}
