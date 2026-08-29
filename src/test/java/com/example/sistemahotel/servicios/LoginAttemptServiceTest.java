package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.repositories.ILoginAttemptRepository;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private ILoginAttemptRepository loginAttemptRepository;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
        ReflectionTestUtils.setField(service, "loginAttemptRepository", loginAttemptRepository);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "lockTimeMinutes", 5);
    }

    @Test
    void noEstaBloqueadoConMenosIntentosQueElMaximo() {
        when(loginAttemptRepository.countFailedAttempts(anyString(), any())).thenReturn(2L);

        assertFalse(service.isBlocked("127.0.0.1"));
    }

    @Test
    void estaBloqueadoAlAlcanzarElMaximoDeIntentos() {
        when(loginAttemptRepository.countFailedAttempts(anyString(), any())).thenReturn(3L);

        assertTrue(service.isBlocked("127.0.0.1"));
    }

    @Test
    void estaBloqueadoPorEncimaDelMaximo() {
        when(loginAttemptRepository.countFailedAttempts(anyString(), any())).thenReturn(10L);

        assertTrue(service.isBlocked("127.0.0.1"));
    }
}
