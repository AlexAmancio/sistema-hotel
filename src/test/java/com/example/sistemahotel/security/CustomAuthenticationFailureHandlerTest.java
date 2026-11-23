package com.example.sistemahotel.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.servicios.LoginAttemptService;

// Defectos D6 y D7: al fallar el inicio de sesión no se imprime el usuario en
// la consola y el mensaje de bloqueo dice "5 minutos" con espacio.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class CustomAuthenticationFailureHandlerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    private CustomAuthenticationFailureHandler handler;
    private final ByteArrayOutputStream consola = new ByteArrayOutputStream();
    private PrintStream original;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "loginAttemptService", loginAttemptService);
        ReflectionTestUtils.setField(handler, "lockTimeMinutes", 5);
        original = System.out;
        System.setOut(new PrintStream(consola, true, StandardCharsets.UTF_8));
        when(loginAttemptService.resolveClientIp()).thenReturn("10.0.0.5");
    }

    @AfterEach
    void restaurar() {
        System.setOut(original);
    }

    private String fallar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("username", "usuario_secreto");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("x"));
        return URLDecoder.decode(response.getRedirectedUrl(), StandardCharsets.UTF_8);
    }

    @Test
    void d6_no_imprime_el_usuario_en_la_consola() throws Exception {
        when(loginAttemptService.isBlocked("10.0.0.5")).thenReturn(false);

        fallar();

        assertFalse(consola.toString(StandardCharsets.UTF_8).contains("usuario_secreto"));
        verify(loginAttemptService).registerAttempt("10.0.0.5", "usuario_secreto", false);
    }

    @Test
    void d7_el_mensaje_de_bloqueo_dice_5_minutos_con_espacio() throws Exception {
        when(loginAttemptService.isBlocked("10.0.0.5")).thenReturn(true);

        assertEquals("/login?error=Vuelva a intentar en 5 minutos.", fallar());
    }

    @Test
    void sin_bloqueo_pide_verificar_las_credenciales() throws Exception {
        when(loginAttemptService.isBlocked("10.0.0.5")).thenReturn(false);

        assertTrue(fallar().contains("Verifique sus credenciales"));
    }
}
