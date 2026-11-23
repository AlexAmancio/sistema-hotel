package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
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

import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IUsuarioRepository;

// Operaciones de mantenimiento de usuarios (HU09 a HU13).
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class UsuarioServiceCrudTest {

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

    private Usuario usuario(String username) {
        Usuario u = new Usuario();
        u.setUsername(username);
        return u;
    }

    @Test
    void listarDevuelveTodosLosUsuarios() {
        when(repositorio.findAll()).thenReturn(List.of(usuario("elon"), usuario("mquispe")));

        assertEquals(2, service.listar().size());
    }

    @Test
    void consultarIdDelegaEnElRepositorio() {
        Usuario u = usuario("mquispe");
        when(repositorio.findById(2)).thenReturn(Optional.of(u));

        assertEquals(Optional.of(u), service.consultarId(2));
    }

    @Test
    void consultarPorNombreDeUsuarioUsaElFinderDerivado() {
        Usuario u = usuario("mquispe");
        when(repositorio.findByUsername("mquispe")).thenReturn(Optional.of(u));

        assertEquals(Optional.of(u), service.consultarpornombreusuario("mquispe"));
    }

    @Test
    void guardarDelegaEnElRepositorio() {
        Usuario u = usuario("mquispe");

        service.guardar(u);

        verify(repositorio).save(u);
    }

    @Test
    void eliminarDelegaEnElRepositorio() {
        service.eliminar(2);

        verify(repositorio).deleteById(2);
    }
}
