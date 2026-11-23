package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.sistemahotel.modelos.LoginAttempt;
import com.example.sistemahotel.repositories.ILoginAttemptRepository;

// Regla RN11: el bloqueo por intentos fallidos se lleva por IP. Estas pruebas
// fijan de dónde se toma la IP y qué se registra en cada intento. La primera
// verifica la corrección del defecto D10: la cabecera X-Forwarded-For se ignora.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceIpTest {

    @Mock
    private ILoginAttemptRepository loginAttemptRepository;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
        ReflectionTestUtils.setField(service, "loginAttemptRepository", loginAttemptRepository);
    }

    @AfterEach
    void limpiarPeticion() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void peticion(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void laCabeceraXForwardedForNoReemplazaLaIpDeLaConexion() {
        // Defecto D10 corregido: el cliente puede escribir esta cabecera, así
        // que no se usa para el bloqueo.
        peticion("10.0.0.1", "203.0.113.5, 10.0.0.1");

        assertEquals("10.0.0.1", service.resolveClientIp());
    }

    @Test
    void sinCabeceraTomaLaIpDeLaConexion() {
        peticion("192.168.1.20", null);

        assertEquals("192.168.1.20", service.resolveClientIp());
    }

    @Test
    void sinPeticionEnCursoDevuelveUnknown() {
        assertEquals("unknown", service.resolveClientIp());
    }

    @Test
    void registraElIntentoFallidoConIpUsuarioYHora() {
        service.registerAttempt("192.168.1.20", "elon", false);

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        LoginAttempt intento = captor.getValue();
        assertEquals("192.168.1.20", intento.getIpAddress());
        assertEquals("elon", intento.getUsername());
        assertFalse(intento.isSuccessful());
        assertNotNull(intento.getAttemptTime());
    }

    @Test
    void siNoLlegaElUsuarioSeRegistraComoUnknown() {
        service.registerAttempt("192.168.1.20", null, true);

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(captor.capture());
        assertEquals("unknown", captor.getValue().getUsername());
        assertTrue(captor.getValue().isSuccessful());
    }
}
