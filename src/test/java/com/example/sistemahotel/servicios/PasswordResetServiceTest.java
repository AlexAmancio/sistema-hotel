package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.PasswordResetToken;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IPasswordResetTokenRepository;
import com.example.sistemahotel.repositories.IUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private IUsuarioRepository usuarioRepositorio;

    @Mock
    private IPasswordResetTokenRepository tokenRepositorio;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService();
        ReflectionTestUtils.setField(service, "usuarioRepositorio", usuarioRepositorio);
        ReflectionTestUtils.setField(service, "tokenRepositorio", tokenRepositorio);
        ReflectionTestUtils.setField(service, "mailSender", mailSender);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "tokenExpiracionMinutos", 30);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8090");
    }

    @Test
    void solicitarReseteoNoHaceNadaSiElCorreoNoExiste() {
        when(usuarioRepositorio.findByCorreo("fantasma@correo.com")).thenReturn(Optional.empty());

        service.solicitarReseteo("fantasma@correo.com");

        verify(tokenRepositorio, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void solicitarReseteoGuardaUnTokenYMandaElCorreoSiElUsuarioExiste() {
        Usuario usuario = new Usuario();
        usuario.setNombres("Elon");
        usuario.setCorreo("musk@gmail.com");
        when(usuarioRepositorio.findByCorreo("musk@gmail.com")).thenReturn(Optional.of(usuario));

        service.solicitarReseteo("musk@gmail.com");

        verify(tokenRepositorio).save(any(PasswordResetToken.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void tokenValidoEsFalsoSiElTokenNoExiste() {
        when(tokenRepositorio.findByToken("no-existe")).thenReturn(Optional.empty());
        assertFalse(service.tokenValido("no-existe"));
    }

    @Test
    void tokenValidoEsFalsoSiElTokenYaFueUsado() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsado(true);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        when(tokenRepositorio.findByToken("usado")).thenReturn(Optional.of(token));

        assertFalse(service.tokenValido("usado"));
    }

    @Test
    void tokenValidoEsFalsoSiElTokenYaExpiro() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsado(false);
        token.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        when(tokenRepositorio.findByToken("expirado")).thenReturn(Optional.of(token));

        assertFalse(service.tokenValido("expirado"));
    }

    @Test
    void tokenValidoEsTrueSiElTokenNoEstaUsadoNiExpirado() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsado(false);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        when(tokenRepositorio.findByToken("vigente")).thenReturn(Optional.of(token));

        assertTrue(service.tokenValido("vigente"));
    }

    @Test
    void restablecerContrasenaDevuelveFalseSiElTokenNoEsValido() {
        when(tokenRepositorio.findByToken("no-existe")).thenReturn(Optional.empty());

        boolean resultado = service.restablecerContrasena("no-existe", "nuevaClave");

        assertFalse(resultado);
        verify(usuarioRepositorio, never()).save(any());
    }

    @Test
    void restablecerContrasenaActualizaElUsuarioYMarcaElTokenComoUsado() {
        Usuario usuario = new Usuario();
        usuario.setContrasena("hashViejo");

        PasswordResetToken token = new PasswordResetToken();
        token.setUsado(false);
        token.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
        token.setUsuario(usuario);

        when(tokenRepositorio.findByToken("vigente")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("nuevaClave")).thenReturn("hashNuevo");

        boolean resultado = service.restablecerContrasena("vigente", "nuevaClave");

        assertTrue(resultado);
        assertTrue(token.isUsado());
        assertTrue("hashNuevo".equals(usuario.getContrasena()));
        verify(usuarioRepositorio).save(usuario);
        verify(tokenRepositorio).save(token);
    }
}
