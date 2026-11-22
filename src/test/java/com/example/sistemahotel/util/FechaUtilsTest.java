package com.example.sistemahotel.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// FechaUtils.mismoDia decide qué estadías llegan o salen hoy (pantalla Hoy,
// regla RN43) y qué ingresos son del día (indicadores, regla RN32).
@Tag("unitaria")
class FechaUtilsTest {

    private Date fecha(int anio, int mes, int dia, int hora, int minuto) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes, dia, hora, minuto, 0);
        return cal.getTime();
    }

    @Test
    void mismoDiaConHorasDistintasEsElMismoDia() {
        assertTrue(FechaUtils.mismoDia(fecha(2026, Calendar.NOVEMBER, 22, 8, 0), fecha(2026, Calendar.NOVEMBER, 22, 23, 59)));
    }

    @Test
    void diasConsecutivosNoSonElMismoDia() {
        assertFalse(FechaUtils.mismoDia(fecha(2026, Calendar.NOVEMBER, 22, 23, 59), fecha(2026, Calendar.NOVEMBER, 23, 0, 0)));
    }

    @Test
    void elMismoDiaDeOtroAnioNoEsElMismoDia() {
        assertFalse(FechaUtils.mismoDia(fecha(2026, Calendar.NOVEMBER, 22, 12, 0), fecha(2025, Calendar.NOVEMBER, 22, 12, 0)));
    }

    @Test
    void siLaPrimeraFechaEsNulaDevuelveFalso() {
        assertFalse(FechaUtils.mismoDia(null, new Date()));
    }

    @Test
    void siLaSegundaFechaEsNulaDevuelveFalso() {
        assertFalse(FechaUtils.mismoDia(new Date(), null));
    }

    @Test
    void siAmbasFechasSonNulasDevuelveFalso() {
        assertFalse(FechaUtils.mismoDia(null, null));
    }
}
