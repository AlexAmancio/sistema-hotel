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

import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.repositories.IHabitacionRepository;

@ExtendWith(MockitoExtension.class)
class HabitacionServiceTest {

    @Mock
    private IHabitacionRepository repositorio;

    private HabitacionService service;

    @BeforeEach
    void setUp() {
        service = new HabitacionService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
    }

    @Test
    void listarDevuelveTodasLasHabitaciones() {
        when(repositorio.findAll()).thenReturn(List.of(new Habitacion(), new Habitacion()));
        assertEquals(2, service.listar().size());
    }

    @Test
    void consultarIdDelegaEnElRepositorio() {
        Habitacion habitacion = new Habitacion();
        habitacion.setId(101);
        when(repositorio.findById(101)).thenReturn(Optional.of(habitacion));
        assertTrue(service.consultarId(101).isPresent());
    }

    @Test
    void guardarDelegaEnElRepositorio() {
        Habitacion habitacion = new Habitacion();
        service.guardar(habitacion);
        verify(repositorio).save(habitacion);
    }

    @Test
    void eliminarDelegaEnElRepositorio() {
        service.eliminar(101);
        verify(repositorio).deleteById(101);
    }

    @Test
    void consultarPornumerohabitacionUsaElFinderDerivado() {
        Habitacion habitacion = new Habitacion();
        habitacion.setNumero("202");
        when(repositorio.findByNumero("202")).thenReturn(Optional.of(habitacion));
        assertTrue(service.consultarPornumerohabitacion("202").isPresent());
    }

    @Test
    void buscarConsultaPorNumeroConteniendoElTexto() {
        when(repositorio.findByNumeroContainingIgnoreCase("10")).thenReturn(List.of(new Habitacion()));
        assertEquals(1, service.buscar("10").size());
    }
}
