package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

// Autocompletado del cliente por DNI (HU17, RN41): el controlador consulta el
// servicio externo apiperu.dev desde el servidor (el token no viaja al
// navegador). El servicio externo se simula: así se prueba la respuesta
// correcta y la caída del servicio sin depender de internet.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class ClientesDniControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<Map<String, String>>> solicitud;

    private ClientesController controller;

    @BeforeEach
    void setUp() {
        controller = new ClientesController();
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(controller, "apiPeruToken", "token-de-prueba");
    }

    @Test
    void conUnDniValidoDevuelveLosDatosDelServicioExterno() {
        String datos = "{\"data\":{\"nombres\":\"CARMEN\"}}";
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class))).thenReturn(datos);

        ResponseEntity<String> respuesta = controller.buscarPorDni("79257439");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(datos, respuesta.getBody());
    }

    @Test
    void laConsultaUsaElDniYElTokenDelServidor() {
        controller.buscarPorDni("79257439");

        verify(restTemplate).postForObject(eq("https://api.apiperu.dev/dni"), solicitud.capture(), eq(String.class));
        assertEquals(Map.of("dni", "79257439"), solicitud.getValue().getBody());
        assertEquals("Bearer token-de-prueba", solicitud.getValue().getHeaders().getFirst("Authorization"));
        assertEquals(MediaType.APPLICATION_JSON, solicitud.getValue().getHeaders().getContentType());
    }

    @Test
    void siElServicioExternoNoRespondeDevuelve502ConUnMensaje() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new ResourceAccessException("sin conexión"));

        ResponseEntity<String> respuesta = controller.buscarPorDni("79257439");

        assertEquals(HttpStatus.BAD_GATEWAY, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().contains("No se pudo consultar el DNI"));
    }

    @Test
    void conLetrasONueveDigitosNoSeConsultaElServicioExterno() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.buscarPorDni("7925743A").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.buscarPorDni("792574390").getStatusCode());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void sinTokenDevuelve503SinConsultarElServicioExterno() {
        ReflectionTestUtils.setField(controller, "apiPeruToken", "");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, controller.buscarPorDni("79257439").getStatusCode());
        verifyNoInteractions(restTemplate);
    }
}
