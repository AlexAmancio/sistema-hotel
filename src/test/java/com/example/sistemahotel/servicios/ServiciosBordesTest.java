package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.TipoDocumento;
import com.example.sistemahotel.repositories.IComprobanteRepository;
import com.example.sistemahotel.repositories.IConsumoRepository;
import com.example.sistemahotel.repositories.ILoginAttemptRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// Casos límite de los servicios de comprobante (RN09), consumos (RN05) e IP del
// bloqueo de intentos (RN11).
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class ServiciosBordesTest {

    @Mock
    private IComprobanteRepository comprobantes;

    @Mock
    private IConsumoRepository consumos;

    @Mock
    private ILoginAttemptRepository intentos;

    @AfterEach
    void limpiarPeticion() {
        RequestContextHolder.resetRequestAttributes();
    }

    private ComprobanteService comprobanteService() {
        ComprobanteService service = new ComprobanteService();
        ReflectionTestUtils.setField(service, "repositorio", comprobantes);
        when(comprobantes.findByRegistroId(1)).thenReturn(Optional.empty());
        when(comprobantes.countBySerie("C001")).thenReturn(0L);
        when(comprobantes.save(any(Comprobante.class))).thenAnswer(inv -> inv.getArgument(0));
        return service;
    }

    private Registro registro(TipoDocumento tipo, Double tarifa, double consumo) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Domo");
        Habitacion habitacion = new Habitacion();
        habitacion.setNumero("D1");
        habitacion.setCategoria(categoria);
        Cliente cliente = new Cliente();
        cliente.setNombres("Ana");
        cliente.setApellidos("Torres");
        cliente.setIdentificacion("X123");
        cliente.setTipoDocumento(tipo);
        Consumo c = new Consumo();
        c.setDescripcion("Café");
        c.setMonto(consumo);
        Registro r = new Registro();
        r.setId(1);
        r.setCliente(cliente);
        r.setHabitacion(habitacion);
        r.setNoches(1);
        r.setTarifa(tarifa);
        r.setConsumos(List.of(c));
        return r;
    }

    @Test
    void sinTipoDeDocumentoElComprobanteLoEmiteComoDni() {
        Comprobante c = comprobanteService().emitir(registro(null, 100.0, 18.0));

        assertTrue(c.getDatosJson().replaceAll("\\s", "").contains("\"tipo_de_documento\":\"DNI\""), c.getDatosJson());
    }

    @Test
    void conPasaporteElComprobanteConservaElTipoDeDocumento() {
        Comprobante c = comprobanteService().emitir(registro(TipoDocumento.PASAPORTE, 100.0, 18.0));

        assertTrue(c.getDatosJson().replaceAll("\\s", "").contains("\"tipo_de_documento\":\"PASAPORTE\""));
    }

    @Test
    void sinTarifaElComprobanteSoloCobraLosConsumos() {
        Comprobante c = comprobanteService().emitir(registro(TipoDocumento.DNI, null, 118.0));

        assertEquals(118.0, c.getTotal());
        assertEquals(100.0, c.getValorVenta());
        assertEquals(18.0, c.getIgv());
    }

    @Test
    void siNoSePuedeArmarElJsonElComprobanteIgualSeEmiteConUnJsonVacio() throws Exception {
        ComprobanteService service = comprobanteService();
        ObjectMapper roto = mock(ObjectMapper.class);
        when(roto.writeValueAsString(any())).thenThrow(new JsonProcessingException("falla") { });
        ReflectionTestUtils.setField(service, "objectMapper", roto);

        Comprobante c = service.emitir(registro(TipoDocumento.DNI, 100.0, 18.0));

        assertEquals("{}", c.getDatosJson());
        assertEquals(118.0, c.getTotal());
    }

    @Test
    void eliminarUnConsumoSinEstadiaDevuelveCero() {
        ConsumoService service = new ConsumoService();
        ReflectionTestUtils.setField(service, "repositorio", consumos);
        Consumo huerfano = new Consumo();
        huerfano.setId(3);
        when(consumos.findById(3)).thenReturn(Optional.of(huerfano));

        assertEquals(0, service.eliminar(3));
        verify(consumos).deleteById(3);
    }

    @Test
    void conLaCabeceraXForwardedForVaciaSeUsaLaIpDeLaConexion() {
        LoginAttemptService service = new LoginAttemptService();
        ReflectionTestUtils.setField(service, "loginAttemptRepository", intentos);
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        peticion.addHeader("X-Forwarded-For", "   ");
        peticion.setRemoteAddr("192.168.1.20");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));

        assertEquals("192.168.1.20", service.resolveClientIp());
    }
}
