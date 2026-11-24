package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Registro;

// Registros sin cliente en el historial y al eliminar, edición que conserva el
// propio documento y alta rápida con campos nulos (no solo vacíos).
@Tag("mockito")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientesControllerRamasTest {

    @Mock private IClienteService service;
    @Mock private IRegistroService registroService;

    @InjectMocks private ClientesController controller;

    private static Cliente cliente(int id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNombres("ana");
        c.setApellidos("rojas");
        c.setIdentificacion("70000001");
        return c;
    }

    @Test
    void elHistorialIgnoraRegistrosSinCliente() {
        when(service.consultarId(1)).thenReturn(Optional.of(cliente(1)));
        when(registroService.listar()).thenReturn(List.of(new Registro()));
        ExtendedModelMap model = new ExtendedModelMap();

        controller.editar(1, model);

        assertEquals(List.of(), model.get("historial"));
    }

    @Test
    void editarConservandoSuPropioDocumentoSeGuarda() {
        Cliente editado = cliente(1);
        when(service.consultarPorIdentificacion("70000001")).thenReturn(Optional.of(cliente(1)));
        when(service.listarPaginado(any())).thenReturn(new PageImpl<>(List.of()));

        controller.guardar(editado, new ExtendedModelMap(), new RedirectAttributesModelMap());

        verify(service).guardar(editado);
    }

    @Test
    void editarConUnDocumentoQueNadieTieneSeGuarda() {
        Cliente editado = cliente(1);
        when(service.consultarPorIdentificacion("70000001")).thenReturn(Optional.empty());
        when(service.listarPaginado(any())).thenReturn(new PageImpl<>(List.of()));

        controller.guardar(editado, new ExtendedModelMap(), new RedirectAttributesModelMap());

        verify(service).guardar(editado);
    }

    @Test
    void seEliminaUnClienteSiNingunRegistroLoTiene() {
        when(registroService.listar()).thenReturn(List.of(new Registro()));

        controller.eliminar(1, new RedirectAttributesModelMap());

        verify(service).eliminar(1);
    }

    @Test
    void elAltaRapidaRechazaNombresApellidosOIdentificacionNulos() {
        assertEquals(HttpStatus.BAD_REQUEST, controller.crearRapido(null, "Rojas", "70000001", null, null).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.crearRapido("Ana", null, "70000001", null, null).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.crearRapido("Ana", "Rojas", null, null, null).getStatusCode());
        verify(service, never()).guardar(any());
    }
}
