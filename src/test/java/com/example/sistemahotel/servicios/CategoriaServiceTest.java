package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.repositories.ICategoriaRepository;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private ICategoriaRepository repositorio;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
    }

    @Test
    void listarDevuelveTodasLasCategoriasDelRepositorio() {
        Categoria c1 = new Categoria();
        c1.setNombre("Habitación Estándar");
        Categoria c2 = new Categoria();
        c2.setNombre("Suite Junior");
        when(repositorio.findAll()).thenReturn(List.of(c1, c2));

        assertEquals(2, service.listar().size());
    }

    @Test
    void consultarIdDelegaEnElRepositorio() {
        Categoria categoria = new Categoria();
        categoria.setId(5);
        when(repositorio.findById(5)).thenReturn(Optional.of(categoria));

        Optional<Categoria> resultado = service.consultarId(5);

        assertTrue(resultado.isPresent());
        assertEquals(5, resultado.get().getId());
    }

    @Test
    void guardarDelegaEnElRepositorio() {
        Categoria categoria = new Categoria();
        service.guardar(categoria);
        verify(repositorio).save(categoria);
    }

    @Test
    void eliminarDelegaEnElRepositorio() {
        service.eliminar(7);
        verify(repositorio).deleteById(7);
    }

    @Test
    void consultarPorNombrecategoriaUsaElFinderDerivado() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Suite Junior");
        when(repositorio.findByNombre("Suite Junior")).thenReturn(Optional.of(categoria));

        Optional<Categoria> resultado = service.consultarPorNombrecategoria("Suite Junior");

        assertTrue(resultado.isPresent());
        assertEquals("Suite Junior", resultado.get().getNombre());
    }
}
