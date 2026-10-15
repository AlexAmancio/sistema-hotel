package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private IUsuarioRepository repositorio;

    @Mock
    private LoginAttemptService loginAttemptService;

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
        ReflectionTestUtils.setField(service, "loginAttemptService", loginAttemptService);
    }

    @Test
    void loadUserByUsernameRechazaSiLaIpEstaBloqueada() {
        when(loginAttemptService.resolveClientIp()).thenReturn("10.0.0.5");
        when(loginAttemptService.isBlocked("10.0.0.5")).thenReturn(true);

        assertThrows(LockedException.class, () -> service.loadUserByUsername("elon"));

        verify(repositorio, never()).findByUsername(any());
    }

    @Test
    void loadUserByUsernameFallaSiElUsuarioNoExiste() {
        when(loginAttemptService.resolveClientIp()).thenReturn("10.0.0.5");
        when(loginAttemptService.isBlocked("10.0.0.5")).thenReturn(false);
        when(repositorio.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("fantasma"));
    }

    @Test
    void loadUserByUsernameDevuelveElUsuarioEnvueltoSiExiste() {
        Usuario usuario = new Usuario();
        usuario.setUsername("elon");
        usuario.setContrasena("hash");
        usuario.setActivo(true);
        usuario.setEsAdmin(true);

        when(loginAttemptService.resolveClientIp()).thenReturn("10.0.0.5");
        when(loginAttemptService.isBlocked("10.0.0.5")).thenReturn(false);
        when(repositorio.findByUsername("elon")).thenReturn(Optional.of(usuario));

        UserDetails resultado = service.loadUserByUsername("elon");

        assertEquals("elon", resultado.getUsername());
    }

    @Test
    void guardarHoraDeCierreDeSesionActualizaElUsuarioCuandoExiste() {
        Usuario usuario = new Usuario();
        usuario.setUsername("elon");
        when(repositorio.findByUsername("elon")).thenReturn(Optional.of(usuario));

        LocalDateTime ahora = LocalDateTime.now();
        service.guardarHoraDeCierreDeSesion("elon", ahora);

        assertEquals(ahora, usuario.getLogoutTime());
        verify(repositorio).save(usuario);
    }

    @Test
    void guardarHoraDeCierreDeSesionNoHaceNadaSiElUsuarioNoExiste() {
        when(repositorio.findByUsername("fantasma")).thenReturn(Optional.empty());

        service.guardarHoraDeCierreDeSesion("fantasma", LocalDateTime.now());

        verify(repositorio, never()).save(any());
    }
}
