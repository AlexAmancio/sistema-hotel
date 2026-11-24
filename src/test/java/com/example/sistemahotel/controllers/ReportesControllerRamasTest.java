package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ui.ExtendedModelMap;

import com.example.sistemahotel.interfaces.ICategoriaService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

// Registros incompletos (sin habitación, sin fecha o sin monto pagado), de 0
// noches o fuera del rango visible en Ocupación y en Reportes.
@Tag("mockito")
class ReportesControllerRamasTest {

    private static Habitacion habitacion(int id) {
        Habitacion h = new Habitacion();
        h.setId(id);
        h.setNumero(String.valueOf(100 + id));
        return h;
    }

    private static Date enDias(int dias) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, dias);
        return c.getTime();
    }

    private static Registro registro(EstadoRegistro estado, Habitacion habitacion, Date checkIn, int noches) {
        Registro r = new Registro();
        r.setEstado(estado);
        r.setHabitacion(habitacion);
        r.setCheckIn(checkIn);
        r.setNoches(noches);
        r.setTarifa(100.0);
        return r;
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Ocupacion {
        @Mock private IHabitacionService habitacionService;
        @Mock private IRegistroService registroService;
        @Mock private ICategoriaService categoriaService;
        @InjectMocks private OcupacionController controller;

        @Test
        void soloDibujaBloquesDeRegistrosCompletosDentroDelRangoYConNoches() {
            Habitacion h1 = habitacion(1);
            when(habitacionService.listar()).thenReturn(List.of(h1));
            when(registroService.listar()).thenReturn(List.of(
                registro(EstadoRegistro.ACTIVA, null, enDias(1), 2),
                registro(EstadoRegistro.ACTIVA, h1, null, 2),
                registro(EstadoRegistro.ACTIVA, h1, enDias(1), 0),
                registro(EstadoRegistro.ACTIVA, h1, enDias(90), 2)));
            ExtendedModelMap model = new ExtendedModelMap();

            controller.ver(null, model);

            List<?> filas = (List<?>) model.get("filas");
            OcupacionController.FilaOcupacion fila = (OcupacionController.FilaOcupacion) filas.get(0);
            assertTrue(fila.bloques.isEmpty());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Dashboard {
        @Mock private IRegistroService registroService;
        @Mock private IHabitacionService habitacionService;
        @InjectMocks private DashboardController controller;

        @Test
        void losIngresosCuentanCeroCuandoNoHayMontoPagadoYSeIgnoranRegistrosIncompletos() {
            Habitacion h1 = habitacion(1);
            when(habitacionService.listar()).thenReturn(List.of(h1));
            when(registroService.idsHabitacionesOcupadasAhora(any())).thenReturn(Set.of());
            Registro hoySinMonto = registro(EstadoRegistro.FINALIZADA, h1, new Date(), 1);
            hoySinMonto.setMontoPagado(null);
            when(registroService.listar()).thenReturn(List.of(
                hoySinMonto,
                registro(EstadoRegistro.FINALIZADA, h1, null, 1),
                registro(EstadoRegistro.FINALIZADA, null, new Date(), 1)));
            ExtendedModelMap model = new ExtendedModelMap();

            controller.dashboard(model);

            assertEquals(0.0, model.get("ingresosHoy"));
        }
    }
}
