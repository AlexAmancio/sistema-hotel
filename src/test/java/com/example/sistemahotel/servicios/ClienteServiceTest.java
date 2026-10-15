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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.repositories.IClienteRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private IClienteRepository repositorio;

    private ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
    }

    @Test
    void listarDevuelveTodosLosClientes() {
        when(repositorio.findAll()).thenReturn(List.of(new Cliente(), new Cliente()));
        assertEquals(2, service.listar().size());
    }

    @Test
    void listarPaginadoDelegaEnElRepositorio() {
        Pageable pageable = PageRequest.of(0, 15);
        Page<Cliente> pagina = new PageImpl<>(List.of(new Cliente()));
        when(repositorio.findAll(pageable)).thenReturn(pagina);

        Page<Cliente> resultado = service.listarPaginado(pageable);

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void consultarIdDelegaEnElRepositorio() {
        Cliente cliente = new Cliente();
        cliente.setId(3);
        when(repositorio.findById(3)).thenReturn(Optional.of(cliente));

        assertTrue(service.consultarId(3).isPresent());
    }

    @Test
    void guardarDelegaEnElRepositorio() {
        Cliente cliente = new Cliente();
        service.guardar(cliente);
        verify(repositorio).save(cliente);
    }

    @Test
    void eliminarDelegaEnElRepositorio() {
        service.eliminar(9);
        verify(repositorio).deleteById(9);
    }

    @Test
    void consultarPorIdentificacionUsaElFinderDerivado() {
        Cliente cliente = new Cliente();
        cliente.setIdentificacion("45678912");
        when(repositorio.findByIdentificacion("45678912")).thenReturn(Optional.of(cliente));

        assertTrue(service.consultarPorIdentificacion("45678912").isPresent());
    }

    @Test
    void buscarConsultaPorNombresApellidosOIdentificacion() {
        when(repositorio.findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCaseOrIdentificacionContainingIgnoreCase(
            "ana", "ana", "ana")).thenReturn(List.of(new Cliente()));

        assertEquals(1, service.buscar("ana").size());
    }
}
