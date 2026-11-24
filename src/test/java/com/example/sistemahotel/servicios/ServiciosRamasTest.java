package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.PasswordResetToken;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IHabitacionRepository;
import com.example.sistemahotel.repositories.IPasswordResetTokenRepository;
import com.example.sistemahotel.repositories.IRegistroRepository;
import com.example.sistemahotel.repositories.IUsuarioRepository;

// Ramas de RegistroService y PasswordResetService que dependen de datos
// incompletos o de una contraseña nula con un token vigente.
@Tag("mockito")
class ServiciosRamasTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Registros {
        @Mock private IRegistroRepository repositorio;
        @Mock private IHabitacionRepository habitacionrepositorio;
        @InjectMocks private RegistroService service;

        @Test
        void elRegistroQueSeEditaNoBloqueaSuPropiaHabitacion() {
            Habitacion h1 = new Habitacion();
            h1.setId(1);
            Registro editando = new Registro();
            editando.setId(9);
            editando.setHabitacion(h1);
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 1);
            editando.setCheckIn(c.getTime());
            editando.setNoches(2);
            when(habitacionrepositorio.findAll()).thenReturn(List.of(h1));
            when(repositorio.findAll()).thenReturn(List.of(editando));

            List<Habitacion> disponibles = service.obtenerHabitacionesDisponibles(c.getTime(), 2, 9);

            assertTrue(disponibles.contains(h1));
        }

        @Test
        void sinRegistroExcluidoUnaEstadiaQueSeCruzaOcupaLaHabitacion() {
            Habitacion h1 = new Habitacion();
            h1.setId(1);
            Registro activo = new Registro();
            activo.setId(9);
            activo.setHabitacion(h1);
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 1);
            activo.setCheckIn(c.getTime());
            activo.setNoches(2);
            when(habitacionrepositorio.findAll()).thenReturn(List.of(h1));
            when(repositorio.findAll()).thenReturn(List.of(activo));

            assertFalse(service.obtenerHabitacionesDisponibles(c.getTime(), 2, null).contains(h1));
            assertFalse(service.obtenerHabitacionesDisponibles(c.getTime(), 2, 4).contains(h1));
        }

        @Test
        void noSeExtiendeAMenosDeUnaNoche() {
            Registro registro = new Registro();
            registro.setId(9);
            registro.setHabitacion(new Habitacion());
            registro.setCheckIn(new Date());
            registro.setNoches(1);
            when(repositorio.findById(9)).thenReturn(Optional.of(registro));

            assertFalse(service.extenderEstadia(9, 0));
        }

        @Test
        void noSeExtiendeUnaEstadiaSinHabitacion() {
            Registro sinHabitacion = new Registro();
            sinHabitacion.setId(9);
            sinHabitacion.setCheckIn(new Date());
            sinHabitacion.setNoches(1);
            when(repositorio.findById(9)).thenReturn(Optional.of(sinHabitacion));

            assertFalse(service.extenderEstadia(9, 3));
            verify(repositorio, never()).save(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Restablecer {
        @Mock private IUsuarioRepository usuarioRepositorio;
        @Mock private IPasswordResetTokenRepository tokenRepositorio;
        @Mock private JavaMailSender mailSender;
        @Mock private PasswordEncoder passwordEncoder;
        @InjectMocks private PasswordResetService service;

        private void tokenVigente() {
            PasswordResetToken token = new PasswordResetToken();
            token.setToken("t");
            token.setUsuario(new Usuario());
            token.setFechaExpiracion(LocalDateTime.now().plusMinutes(10));
            when(tokenRepositorio.findByToken("t")).thenReturn(Optional.of(token));
        }

        @Test
        void unTokenVigenteConContrasenaNulaNoCambiaNada() {
            tokenVigente();

            assertFalse(service.restablecerContrasena("t", null));
            verify(usuarioRepositorio, never()).save(any());
        }

        @Test
        void unTokenVigenteConContrasenaCortaNoCambiaNada() {
            tokenVigente();

            assertFalse(service.restablecerContrasena("t", " ab "));
            verify(usuarioRepositorio, never()).save(any());
        }
    }
}
