package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IComprobanteRepository;

// Regla RN09: el comprobante usa la serie C001, incluye alojamiento y consumos,
// los precios incluyen IGV (valor de venta = total / 1,18) y guarda un JSON
// listo para un operador homologado (HU59 y HU60).
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class ComprobanteServiceDetalleTest {

    @Mock
    private IComprobanteRepository repositorio;

    private ComprobanteService service;
    private Registro registro;

    @BeforeEach
    void setUp() {
        service = new ComprobanteService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);

        Categoria categoria = new Categoria();
        categoria.setNombre("Habitación Matrimonial");
        Habitacion habitacion = new Habitacion();
        habitacion.setNumero("101");
        habitacion.setCategoria(categoria);
        Cliente cliente = new Cliente();
        cliente.setNombres("Carmen");
        cliente.setApellidos("Herrera Rojas");
        cliente.setIdentificacion("79257439");
        Consumo minibar = new Consumo();
        minibar.setDescripcion("Minibar");
        minibar.setMonto(20.0);

        registro = new Registro();
        registro.setId(103);
        registro.setCliente(cliente);
        registro.setHabitacion(habitacion);
        registro.setNoches(2);
        registro.setTarifa(360.0);
        registro.setConsumos(List.of(minibar));

        when(repositorio.findByRegistroId(103)).thenReturn(Optional.empty());
        when(repositorio.countBySerie("C001")).thenReturn(4L);
        when(repositorio.save(any(Comprobante.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void usaLaSerieC001ConElSiguienteCorrelativo() {
        Comprobante comprobante = service.emitir(registro);

        assertEquals("C001", comprobante.getSerie());
        assertEquals("C001-00000005", comprobante.getNumeroCompleto());
    }

    @Test
    void elTotalIncluyeAlojamientoYConsumos() {
        assertEquals(380.0, service.emitir(registro).getTotal());
    }

    @Test
    void elValorDeVentaMasElIgvDaElTotal() {
        Comprobante comprobante = service.emitir(registro);

        assertEquals(322.03, comprobante.getValorVenta());
        assertEquals(57.97, comprobante.getIgv());
        assertEquals(comprobante.getTotal(), comprobante.getValorVenta() + comprobante.getIgv(), 0.001);
    }

    @Test
    void registraLaFechaDeEmisionYQuedaEnlazadoALaEstadia() {
        Comprobante comprobante = service.emitir(registro);

        assertNotNull(comprobante.getFechaEmision());
        assertEquals(103, comprobante.getRegistro().getId());
    }

    @Test
    void elJsonTraeClienteMonedaYCadaConsumo() {
        String json = service.emitir(registro).getDatosJson();

        assertTrue(json.replace(" ", "").contains("\"moneda\":\"PEN\""));
        assertTrue(json.contains("79257439"));
        assertTrue(json.contains("Carmen Herrera Rojas"));
        assertTrue(json.contains("Minibar"));
        assertTrue(json.contains("Habitación 101"));
    }
}
