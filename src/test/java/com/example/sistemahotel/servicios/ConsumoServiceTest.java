package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IConsumoRepository;

@ExtendWith(MockitoExtension.class)
class ConsumoServiceTest {

    @Mock
    private IConsumoRepository repositorio;

    private ConsumoService service;

    @BeforeEach
    void setUp() {
        service = new ConsumoService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
    }

    @Test
    void listarPorRegistroDelegaEnElRepositorio() {
        when(repositorio.findByRegistroId(6)).thenReturn(List.of(new Consumo()));
        assertEquals(1, service.listarPorRegistro(6).size());
    }

    @Test
    void agregarArmaElConsumoYLoGuarda() {
        Registro registro = new Registro();
        registro.setId(6);

        ArgumentCaptor<Consumo> captor = ArgumentCaptor.forClass(Consumo.class);
        service.agregar(registro, "Minibar", 25.5);

        verify(repositorio).save(captor.capture());
        Consumo guardado = captor.getValue();
        assertEquals("Minibar", guardado.getDescripcion());
        assertEquals(25.5, guardado.getMonto(), 0.001);
        assertEquals(registro, guardado.getRegistro());
    }

    @Test
    void eliminarDevuelveElIdDelRegistroAsociado() {
        Registro registro = new Registro();
        registro.setId(6);
        Consumo consumo = new Consumo();
        consumo.setId(3);
        consumo.setRegistro(registro);
        when(repositorio.findById(3)).thenReturn(Optional.of(consumo));

        int registroId = service.eliminar(3);

        assertEquals(6, registroId);
        verify(repositorio).deleteById(3);
    }

    @Test
    void eliminarDevuelveCeroSiElConsumoYaNoExiste() {
        when(repositorio.findById(99)).thenReturn(Optional.empty());

        int registroId = service.eliminar(99);

        assertEquals(0, registroId);
        verify(repositorio).deleteById(99);
    }
}
