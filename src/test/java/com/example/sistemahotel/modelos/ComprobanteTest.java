package com.example.sistemahotel.modelos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Regla RN09: el comprobante se identifica con la serie y un correlativo de
// 8 dígitos completado con ceros, como C001-00000005.
@Tag("unitaria")
class ComprobanteTest {

    @Test
    void elNumeroCompletoUneSerieYCorrelativoConOchoDigitos() {
        Comprobante comprobante = new Comprobante();
        comprobante.setSerie("C001");
        comprobante.setCorrelativo(5);
        assertEquals("C001-00000005", comprobante.getNumeroCompleto());
    }

    @Test
    void elPrimerComprobanteEsElCorrelativoUno() {
        Comprobante comprobante = new Comprobante();
        comprobante.setSerie("C001");
        comprobante.setCorrelativo(1);
        assertEquals("C001-00000001", comprobante.getNumeroCompleto());
    }

    @Test
    void unCorrelativoGrandeNoSeRecorta() {
        Comprobante comprobante = new Comprobante();
        comprobante.setSerie("C001");
        comprobante.setCorrelativo(12345678);
        assertEquals("C001-12345678", comprobante.getNumeroCompleto());
    }
}
