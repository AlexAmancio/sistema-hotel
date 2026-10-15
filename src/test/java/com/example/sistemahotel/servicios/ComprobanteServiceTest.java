package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IComprobanteRepository;

// Cubre la lógica real de emisión: idempotencia (no duplica si ya existe un
// comprobante para el registro), el cálculo del correlativo (countBySerie + 1,
// el mismo cálculo que el registro de riesgos del proyecto marca como
// condición de carrera) y el desglose de IGV sobre el total cobrado.
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceTest {

    @Mock
    private IComprobanteRepository repositorio;

    private ComprobanteService service;

    private Registro registro;

    @BeforeEach
    void setUp() {
        service = new ComprobanteService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);

        Categoria categoria = new Categoria();
        categoria.setNombre("Habitación Estándar");

        Habitacion habitacion = new Habitacion();
        habitacion.setNumero("101");
        habitacion.setCategoria(categoria);

        Cliente cliente = new Cliente();
        cliente.setNombres("Ana");
        cliente.setApellidos("Torres");
        cliente.setIdentificacion("45678912");

        registro = new Registro();
        registro.setId(6);
        registro.setCliente(cliente);
        registro.setHabitacion(habitacion);
        registro.setNoches(2);
        registro.setTarifa(236.0); // elegido para que el IGV salga exacto: 236 / 1.18 = 200
    }

    @Test
    void emitirDevuelveElComprobanteExistenteSinCrearOtro() {
        Comprobante existente = new Comprobante();
        existente.setId(1);
        when(repositorio.findByRegistroId(6)).thenReturn(Optional.of(existente));

        Comprobante resultado = service.emitir(registro);

        assertSame(existente, resultado);
        verify(repositorio, never()).countBySerie(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void emitirCalculaElCorrelativoComoElConteoMasUno() {
        when(repositorio.findByRegistroId(6)).thenReturn(Optional.empty());
        when(repositorio.countBySerie("C001")).thenReturn(4L);
        when(repositorio.save(any(Comprobante.class))).thenAnswer(inv -> inv.getArgument(0));

        Comprobante resultado = service.emitir(registro);

        assertEquals(5, resultado.getCorrelativo());
        assertEquals("C001", resultado.getSerie());
    }

    @Test
    void emitirDesglosaElIgvSobreElTotalCobrado() {
        when(repositorio.findByRegistroId(6)).thenReturn(Optional.empty());
        when(repositorio.countBySerie("C001")).thenReturn(0L);
        when(repositorio.save(any(Comprobante.class))).thenAnswer(inv -> inv.getArgument(0));

        Comprobante resultado = service.emitir(registro);

        assertEquals(236.0, resultado.getTotal(), 0.001);
        assertEquals(200.0, resultado.getValorVenta(), 0.001);
        assertEquals(36.0, resultado.getIgv(), 0.001);
    }

    @Test
    void emitirGuardaElSnapshotJsonDelComprobante() {
        when(repositorio.findByRegistroId(6)).thenReturn(Optional.empty());
        when(repositorio.countBySerie("C001")).thenReturn(0L);
        ArgumentCaptor<Comprobante> captor = ArgumentCaptor.forClass(Comprobante.class);
        when(repositorio.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.emitir(registro);

        String json = captor.getValue().getDatosJson();
        assertTrue(json.contains("\"numero\""));
        assertTrue(json.contains("45678912"));
    }
}
