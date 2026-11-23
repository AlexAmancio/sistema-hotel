package com.example.sistemahotel.modelos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Regla RN04: el precio por noche depende de la fecha de ingreso. Si la fecha
// cae dentro de la ventana de temporada alta de la categoría se cobra el precio
// de temporada alta; fuera de la ventana, o si la temporada no está configurada,
// se cobra el precio base.
@Tag("unitaria")
class CategoriaTest {

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setPrecio(180.0);
        categoria.setPrecioTemporadaAlta(250.0);
        categoria.setTemporadaAltaInicio(fecha(2026, Calendar.DECEMBER, 20));
        categoria.setTemporadaAltaFin(fecha(2027, Calendar.JANUARY, 5));
    }

    private Date fecha(int anio, int mes, int dia) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(anio, mes, dia, 0, 0, 0);
        return cal.getTime();
    }

    @Test
    void cobraElPrecioBaseFueraDeLaTemporadaAlta() {
        assertEquals(180.0, categoria.precioParaFecha(fecha(2026, Calendar.NOVEMBER, 22)));
    }

    @Test
    void cobraElPrecioDeTemporadaAltaDentroDeLaVentana() {
        assertEquals(250.0, categoria.precioParaFecha(fecha(2026, Calendar.DECEMBER, 28)));
    }

    @Test
    void elPrimerDiaDeLaTemporadaYaCobraElPrecioAlto() {
        assertEquals(250.0, categoria.precioParaFecha(fecha(2026, Calendar.DECEMBER, 20)));
    }

    @Test
    void elUltimoDiaDeLaTemporadaTodaviaCobraElPrecioAlto() {
        assertEquals(250.0, categoria.precioParaFecha(fecha(2027, Calendar.JANUARY, 5)));
    }

    @Test
    void elDiaSiguienteAlFinDeLaTemporadaVuelveAlPrecioBase() {
        assertEquals(180.0, categoria.precioParaFecha(fecha(2027, Calendar.JANUARY, 6)));
    }

    @Test
    void sinFechaDeIngresoSeCobraElPrecioBase() {
        assertEquals(180.0, categoria.precioParaFecha(null));
    }

    @Test
    void sinPrecioDeTemporadaAltaSeCobraElPrecioBase() {
        categoria.setPrecioTemporadaAlta(null);
        assertEquals(180.0, categoria.precioParaFecha(fecha(2026, Calendar.DECEMBER, 28)));
    }

    @Test
    void sinVentanaDeTemporadaConfiguradaSeCobraElPrecioBase() {
        categoria.setTemporadaAltaInicio(null);
        categoria.setTemporadaAltaFin(null);
        assertEquals(180.0, categoria.precioParaFecha(fecha(2026, Calendar.DECEMBER, 28)));
    }
}
