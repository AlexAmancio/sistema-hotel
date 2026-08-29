package com.example.sistemahotel.modelos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class RegistroTest {

    @Test
    void totalEstadiaSumaTarifaYConsumos() {
        Registro registro = new Registro();
        registro.setTarifa(100.0);

        Consumo consumo1 = new Consumo();
        consumo1.setMonto(20.0);
        Consumo consumo2 = new Consumo();
        consumo2.setMonto(5.5);
        registro.setConsumos(List.of(consumo1, consumo2));

        assertEquals(125.5, registro.getTotalEstadia());
    }

    @Test
    void totalEstadiaSinConsumosEsSoloLaTarifa() {
        Registro registro = new Registro();
        registro.setTarifa(80.0);
        registro.setConsumos(null);

        assertEquals(80.0, registro.getTotalEstadia());
    }

    @Test
    void saldoPendienteDescuentaLoPagado() {
        Registro registro = new Registro();
        registro.setTarifa(150.0);
        registro.setMontoPagado(50.0);

        assertEquals(100.0, registro.getSaldoPendiente());
    }

    @Test
    void saldoPendienteEsCeroCuandoSePagoTodo() {
        Registro registro = new Registro();
        registro.setTarifa(150.0);
        registro.setMontoPagado(150.0);

        assertEquals(0.0, registro.getSaldoPendiente());
    }

    @Test
    void calcularCheckoutEstimadoSumaNochesAlMediodia() {
        Registro registro = new Registro();
        Calendar checkIn = Calendar.getInstance();
        checkIn.set(2026, Calendar.JANUARY, 1, 15, 30, 0);
        registro.setCheckIn(checkIn.getTime());
        registro.setNoches(3);

        Date checkoutEstimado = registro.calcularCheckoutEstimado();

        Calendar esperado = Calendar.getInstance();
        esperado.set(2026, Calendar.JANUARY, 4, 12, 0, 0);
        esperado.set(Calendar.MILLISECOND, 0);

        assertEquals(esperado.getTime(), checkoutEstimado);
    }

    @Test
    void checkoutEstimadoAmigableDiceVencidoSiYaPaso() {
        Registro registro = new Registro();
        Calendar checkIn = Calendar.getInstance();
        checkIn.add(Calendar.DAY_OF_MONTH, -5);
        registro.setCheckIn(checkIn.getTime());
        registro.setNoches(1);

        assertEquals("Vencido", registro.calcularCheckoutEstimadoAmigable());
    }

    @Test
    void checkoutEstimadoAmigableNoDiceVencidoSiFalta() {
        Registro registro = new Registro();
        Calendar checkIn = Calendar.getInstance();
        registro.setCheckIn(checkIn.getTime());
        registro.setNoches(5);

        assertTrue(registro.calcularCheckoutEstimadoAmigable().contains("días"));
    }
}
