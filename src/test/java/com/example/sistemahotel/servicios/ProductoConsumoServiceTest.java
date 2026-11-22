package com.example.sistemahotel.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.repositories.IProductoConsumoRepository;

// Catálogo de productos de consumo (HU54 a HU58). La regla RN28 dice que solo
// los productos activos aparecen como atajos, y la lista se muestra por nombre.
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
class ProductoConsumoServiceTest {

    @Mock
    private IProductoConsumoRepository repositorio;

    private ProductoConsumoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoConsumoService();
        ReflectionTestUtils.setField(service, "repositorio", repositorio);
    }

    private ProductoConsumo producto(String nombre, boolean activo) {
        ProductoConsumo p = new ProductoConsumo();
        p.setNombre(nombre);
        p.setActivo(activo);
        return p;
    }

    @Test
    void listarDevuelveTodosLosProductosOrdenadosPorNombre() {
        when(repositorio.findAllByOrderByNombreAsc()).thenReturn(List.of(producto("Lavandería", true), producto("Minibar", false)));

        assertEquals(2, service.listar().size());
    }

    @Test
    void listarActivosDevuelveSoloLosAtajosDisponibles() {
        when(repositorio.findByActivoTrueOrderByNombreAsc()).thenReturn(List.of(producto("Minibar", true)));

        List<ProductoConsumo> activos = service.listarActivos();

        assertEquals(1, activos.size());
        assertTrue(activos.get(0).isActivo());
    }

    @Test
    void consultarIdDelegaEnElRepositorio() {
        ProductoConsumo minibar = producto("Minibar", true);
        when(repositorio.findById(3)).thenReturn(Optional.of(minibar));

        assertEquals(Optional.of(minibar), service.consultarId(3));
    }

    @Test
    void guardarDelegaEnElRepositorio() {
        ProductoConsumo minibar = producto("Minibar", true);

        service.guardar(minibar);

        verify(repositorio).save(minibar);
    }

    @Test
    void eliminarDelegaEnElRepositorio() {
        service.eliminar(3);

        verify(repositorio).deleteById(3);
    }
}
