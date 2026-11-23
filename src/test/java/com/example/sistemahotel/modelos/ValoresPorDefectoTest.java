package com.example.sistemahotel.modelos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

// Valores iniciales y catálogos (enumeraciones) del modelo de datos.
@Tag("unitaria")
class ValoresPorDefectoTest {

    @Test
    void unaHabitacionNuevaEstaDisponible() {
        assertEquals(EstadoHabitacion.DISPONIBLE, new Habitacion().getEstado());
    }

    @Test
    void unClienteNuevoUsaDniComoTipoDeDocumento() {
        assertEquals(TipoDocumento.DNI, new Cliente().getTipoDocumento());
    }

    @Test
    void unUsuarioNuevoEstaActivoYNoEsAdministrador() {
        Usuario usuario = new Usuario();
        assertTrue(usuario.isActivo());
        assertEquals(false, usuario.isEsAdmin());
    }

    @Test
    void unProductoNuevoEstaActivoComoAtajo() {
        assertTrue(new ProductoConsumo().isActivo());
    }

    @Test
    void unConsumoNuevoRegistraSuFecha() {
        assertTrue(new Consumo().getFecha() != null);
    }

    @Test
    void losEstadosDeUnRegistroSonLosCuatroDelCicloDeLaEstadia() {
        assertArrayEquals(new EstadoRegistro[] {EstadoRegistro.ACTIVA, EstadoRegistro.FINALIZADA, EstadoRegistro.CANCELADA, EstadoRegistro.NO_SHOW},
            EstadoRegistro.values());
    }

    @Test
    void losEstadosDeUnaHabitacionSonCuatro() {
        assertArrayEquals(new EstadoHabitacion[] {EstadoHabitacion.DISPONIBLE, EstadoHabitacion.OCUPADA, EstadoHabitacion.LIMPIEZA, EstadoHabitacion.MANTENIMIENTO},
            EstadoHabitacion.values());
    }

    @Test
    void losMetodosDePagoSonEfectivoTarjetaYTransferencia() {
        assertArrayEquals(new MetodoPago[] {MetodoPago.EFECTIVO, MetodoPago.TARJETA, MetodoPago.TRANSFERENCIA}, MetodoPago.values());
    }

    @Test
    void losTiposDeDocumentoTienenSuEtiquetaParaLaPantalla() {
        assertEquals("DNI", TipoDocumento.DNI.getEtiqueta());
        assertEquals("Pasaporte", TipoDocumento.PASAPORTE.getEtiqueta());
        assertEquals("Carné de extranjería", TipoDocumento.CARNE_EXTRANJERIA.getEtiqueta());
    }
}
