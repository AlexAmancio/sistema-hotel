package com.example.sistemahotel.modelos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Regla RN05: total de la estadía = tarifa + consumos; saldo pendiente =
// total - monto pagado. Casos límite que no cubría RegistroTest.
@Tag("unitaria")
class RegistroCalculosTest {

    private Consumo consumo(double monto) {
        Consumo consumo = new Consumo();
        consumo.setMonto(monto);
        return consumo;
    }

    @Test
    void sinListaDeConsumosElTotalDeConsumosEsCero() {
        assertEquals(0.0, new Registro().getTotalConsumos());
    }

    @Test
    void elTotalDeConsumosSumaTodosLosConsumos() {
        Registro registro = new Registro();
        registro.setConsumos(List.of(consumo(20.0), consumo(28.0), consumo(40.0)));
        assertEquals(88.0, registro.getTotalConsumos());
    }

    @Test
    void sinTarifaElTotalEsSoloLosConsumos() {
        Registro registro = new Registro();
        registro.setTarifa(null);
        registro.setConsumos(List.of(consumo(20.0)));
        assertEquals(20.0, registro.getTotalEstadia());
    }

    @Test
    void sinMontoPagadoElSaldoEsElTotal() {
        Registro registro = new Registro();
        registro.setTarifa(360.0);
        registro.setMontoPagado(null);
        assertEquals(360.0, registro.getSaldoPendiente());
    }

    @Test
    void unPagoMayorAlTotalDejaElSaldoNegativo() {
        Registro registro = new Registro();
        registro.setTarifa(360.0);
        registro.setMontoPagado(400.0);
        assertEquals(-40.0, registro.getSaldoPendiente());
    }

    @Test
    void unRegistroNuevoEmpiezaActivoPagadoEnCeroYEnEfectivo() {
        Registro registro = new Registro();
        assertEquals(EstadoRegistro.ACTIVA, registro.getEstado());
        assertEquals(0.0, registro.getMontoPagado());
        assertEquals(MetodoPago.EFECTIVO, registro.getMetodoPago());
    }
}
