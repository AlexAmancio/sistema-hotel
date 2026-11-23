package com.example.sistemahotel.modelos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Casos límite de los cálculos de los modelos que no cubrían las demás pruebas
// unitarias: tiempo restante de menos de un día (RN43), temporada alta con la
// ventana a medias (RN02) y montos con valores vacíos (RN07).
@Tag("unitaria")
class CasosLimiteModelosTest {

    private Date hoyALas(int hora) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hora);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    void conMenosDeUnDiaParaSalirElTiempoRestanteNoMuestraDias() {
        // El checkout siempre es a las 12:00: antes del mediodía vence hoy
        // (0 noches desde hoy) y después vence mañana (1 noche). En ambos
        // casos faltan menos de 24 horas.
        Registro registro = new Registro();
        registro.setCheckIn(hoyALas(8));
        registro.setNoches(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 12 ? 0 : 1);

        String restante = registro.calcularCheckoutEstimadoAmigable();

        assertFalse(restante.contains("días"), restante);
        assertTrue(restante.matches("\\d{1,2} h \\d{1,2} m "), restante);
    }

    @Test
    void conVariosDiasParaSalirElTiempoRestanteMuestraLosDias() {
        Registro registro = new Registro();
        registro.setCheckIn(hoyALas(8));
        registro.setNoches(4);

        assertTrue(registro.calcularCheckoutEstimadoAmigable().matches("[34] días \\d{1,2} h \\d{1,2} m "));
    }

    @Test
    void elCheckoutEstimadoEsAlMediodiaAunqueElIngresoFueraDeNoche() {
        Registro registro = new Registro();
        registro.setCheckIn(hoyALas(23));
        registro.setNoches(1);

        Calendar salida = Calendar.getInstance();
        salida.setTime(registro.calcularCheckoutEstimado());

        assertEquals(12, salida.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, salida.get(Calendar.MINUTE));
    }

    @Test
    void conSoloElInicioDeTemporadaConfiguradoSeCobraElPrecioBase() {
        Categoria categoria = new Categoria();
        categoria.setPrecio(300.0);
        categoria.setPrecioTemporadaAlta(380.0);
        categoria.setTemporadaAltaInicio(hoyALas(0));

        assertEquals(300.0, categoria.precioParaFecha(new Date()));
    }

    @Test
    void conSoloElFinDeTemporadaConfiguradoSeCobraElPrecioBase() {
        Categoria categoria = new Categoria();
        categoria.setPrecio(300.0);
        categoria.setPrecioTemporadaAlta(380.0);
        categoria.setTemporadaAltaFin(hoyALas(23));

        assertEquals(300.0, categoria.precioParaFecha(new Date()));
    }

    @Test
    void sinMontoPagadoElSaldoEsElTotalDeLaEstadia() {
        Consumo consumo = new Consumo();
        consumo.setMonto(25.0);
        Registro registro = new Registro();
        registro.setTarifa(200.0);
        registro.setMontoPagado(null);
        registro.setConsumos(List.of(consumo));

        assertEquals(225.0, registro.getSaldoPendiente());
    }

    @Test
    void sinTarifaElTotalSonSoloLosConsumos() {
        Consumo consumo = new Consumo();
        consumo.setMonto(25.0);
        Registro registro = new Registro();
        registro.setTarifa(null);
        registro.setConsumos(List.of(consumo));

        assertEquals(25.0, registro.getTotalEstadia());
    }

    @Test
    void unPagoMayorAlTotalDejaSaldoNegativoAFavorDelHuesped() {
        Registro registro = new Registro();
        registro.setTarifa(200.0);
        registro.setMontoPagado(250.0);

        assertEquals(-50.0, registro.getSaldoPendiente());
    }

    @Test
    void elIntentoDeLoginGuardaTodosSusDatos() {
        LocalDateTime ahora = LocalDateTime.now();
        LoginAttempt intento = new LoginAttempt();
        intento.setId(7);
        intento.setIpAddress("10.0.0.9");
        intento.setUsername("mquispe");
        intento.setAttemptTime(ahora);
        intento.setSuccessful(true);

        assertEquals(7, intento.getId());
        assertEquals("10.0.0.9", intento.getIpAddress());
        assertEquals("mquispe", intento.getUsername());
        assertEquals(ahora, intento.getAttemptTime());
        assertTrue(intento.isSuccessful());
    }
}
