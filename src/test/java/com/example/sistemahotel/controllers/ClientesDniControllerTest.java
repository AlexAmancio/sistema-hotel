package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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

    private ClientesController controller;

    @BeforeEach
    void setUp() {
        controller = new ClientesController();
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(controller, "apiPeruToken", "token-de-prueba");
    }

    @Test
    void conUnDniValidoDevuelveLosDatosDelServicioExterno() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("{\"nombres\":\"CARMEN\"}");

        ResponseEntity<String> respuesta = controller.buscarPorDni("79257439");

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("{\"nombres\":\"CARMEN\"}", respuesta.getBody());
    }

    @Test
    void laConsultaUsaElDniYElTokenDelServidor() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("{}");

        controller.buscarPorDni("79257439");

        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(url.capture(), eq(String.class));
        assertEquals("https://apiperu.dev/api/dni/79257439?api_token=token-de-prueba", url.getValue());
    }

    @Test
    void siElServicioExternoNoRespondeDevuelve502ConUnMensaje() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new ResourceAccessException("sin conexión"));

        ResponseEntity<String> respuesta = controller.buscarPorDni("79257439");

        assertEquals(HttpStatus.BAD_GATEWAY, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().contains("No se pudo consultar el DNI"));
    }

    @Test
    void conLetrasONueveDigitosNoSeConsultaElServicioExterno() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.buscarPorDni("7925743A").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.buscarPorDni("792574390").getStatusCode());
        verify(restTemplate, never()).getForObject(anyString(), eq(String.class));
    }
}
