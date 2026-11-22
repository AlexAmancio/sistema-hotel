package com.example.sistemahotel.modelos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// El total de una reserva de grupo es la suma del total de cada estadía
// (tarifa más consumos) de las habitaciones que la componen.
@Tag("unitaria")
class ReservaTest {

    private Registro estadia(double tarifa, double... consumos) {
        Registro registro = new Registro();
        registro.setTarifa(tarifa);
        registro.setConsumos(java.util.Arrays.stream(consumos).mapToObj(monto -> {
            Consumo consumo = new Consumo();
            consumo.setMonto(monto);
            return consumo;
        }).toList());
        return registro;
    }

    @Test
    void elTotalSumaLasEstadiasDeTodasLasHabitaciones() {
        Reserva reserva = new Reserva();
        reserva.setRegistros(List.of(estadia(780.0), estadia(780.0), estadia(780.0)));
        assertEquals(2340.0, reserva.getTotal());
    }

    @Test
    void elTotalIncluyeLosConsumosDeCadaHabitacion() {
        Reserva reserva = new Reserva();
        reserva.setRegistros(List.of(estadia(360.0, 20.0), estadia(360.0)));
        assertEquals(740.0, reserva.getTotal());
    }

    @Test
    void unaReservaSinRegistrosTotalizaCero() {
        assertEquals(0.0, new Reserva().getTotal());
    }

    @Test
    void unaReservaConListaVaciaTotalizaCero() {
        Reserva reserva = new Reserva();
        reserva.setRegistros(List.of());
        assertEquals(0.0, reserva.getTotal());
    }
}
