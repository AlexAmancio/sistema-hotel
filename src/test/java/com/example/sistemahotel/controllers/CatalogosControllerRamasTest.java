package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.sistemahotel.interfaces.ICategoriaService;
import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IReservaService;
import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IPasswordResetService;
import com.example.sistemahotel.interfaces.IProductoConsumoService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Casos de datos incompletos y parámetros nulos en Habitaciones, Categorías,
// Productos de consumo, Perfil, Login y Reservas.
@Tag("mockito")
class CatalogosControllerRamasTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Habitaciones {
        @Mock private IHabitacionService service;
        @Mock private ICategoriaService categoriaService;
        @Mock private IRegistroService registroService;
        @InjectMocks private HabitacionesController controller;

        @Test
        void seEliminaSiNingunRegistroTieneEsaHabitacion() {
            when(registroService.listar()).thenReturn(List.of(new Registro()));

            controller.eliminar(3, new RedirectAttributesModelMap());

            verify(service).eliminar(3);
        }

        @Test
        void conFechaVaciaLaTarifaUsaElPrecioBase() {
            Categoria categoria = new Categoria();
            categoria.setPrecio(150);
            Habitacion habitacion = new Habitacion();
            habitacion.setCategoria(categoria);
            when(service.consultarId(3)).thenReturn(Optional.of(habitacion));

            assertEquals(300.0, controller.calcularTarifa(3, 2, "").getBody());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Categorias {
        @Mock private ICategoriaService service;
        @Mock private IHabitacionService habitacionService;
        @InjectMocks private CategoriasController controller;

        @Test
        void seEliminaSiLasHabitacionesNoTienenEsaCategoria() {
            when(habitacionService.listar()).thenReturn(List.of(new Habitacion()));

            controller.eliminar(2, new RedirectAttributesModelMap());

            verify(service).eliminar(2);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ProductosConsumo {
        @Mock private IProductoConsumoService service;
        @Mock private IConsumoService consumoService;
        @InjectMocks private ProductosConsumoController controller;

        @Test
        void sinSesionNoSeMuestranAccionesDeAdministrador() {
            SecurityContextHolder.clearContext();
            ExtendedModelMap model = new ExtendedModelMap();

            controller.listar(model);

            assertEquals(false, model.get("esAdmin"));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Perfil {
        @Mock private IUsuarioService usuarioService;
        @Mock private PasswordEncoder passwordEncoder;
        @InjectMocks private PerfilController controller;

        @AfterEach
        void limpiar() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void siElUsuarioLogueadoYaNoExisteSeLanzaUnError() {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("borrado", "x"));
            when(usuarioService.consultarpornombreusuario("borrado")).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> controller.ver(new ExtendedModelMap()));
        }

        @Test
        void unaContrasenaNuevaNulaSeRechaza() {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("elon", "x"));
            Usuario usuario = new Usuario();
            usuario.setContrasena("hash");
            when(usuarioService.consultarpornombreusuario("elon")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

            String vista = controller.cambiarContrasena("actual", null, null, redirect);

            assertEquals("redirect:/perfil/", vista);
            assertEquals("La nueva contraseña no puede estar vacía.", redirect.getFlashAttributes().get("error"));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Login {
        @Mock private IPasswordResetService passwordResetService;
        @InjectMocks private LoginController controller;

        @Test
        void unaContrasenaNulaSeRechazaYElTokenInvalidoNoMuestraElFormulario() {
            when(passwordResetService.tokenValido("t")).thenReturn(false);
            ExtendedModelMap model = new ExtendedModelMap();

            controller.guardarNuevaContrasena("t", null, model);

            assertEquals("invalido", model.get("estado"));
        }

        @Test
        void unaContrasenaCortaConTokenValidoVuelveAMostrarElFormulario() {
            when(passwordResetService.tokenValido("t")).thenReturn(true);
            ExtendedModelMap model = new ExtendedModelMap();

            controller.guardarNuevaContrasena("t", "ab", model);

            assertEquals("formulario", model.get("estado"));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @MockitoSettings(strictness = Strictness.LENIENT)
    class Reservas {
        @Mock private IReservaService reservaService;
        @Mock private IClienteService clienteService;
        @Mock private IUsuarioService usuarioService;
        @InjectMocks private ReservaController controller;

        @AfterEach
        void limpiar() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void conUnClienteInexistenteVuelveAlFormularioConElError() {
            when(clienteService.consultarId(5)).thenReturn(Optional.empty());
            ExtendedModelMap model = new ExtendedModelMap();

            controller.guardar(5, "2026-11-30T14:00", 2, List.of(1), Map.of(), model);

            assertEquals("Cliente no encontrado.", model.get("error"));
        }

        @Test
        void conUnUsuarioLogueadoInexistenteVuelveAlFormularioConElError() {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("borrado", "x"));
            when(clienteService.consultarId(5)).thenReturn(Optional.of(new Cliente()));
            when(usuarioService.consultarpornombreusuario("borrado")).thenReturn(Optional.empty());
            ExtendedModelMap model = new ExtendedModelMap();

            controller.guardar(5, "2026-11-30T14:00", 2, List.of(1), Map.of(), model);

            assertEquals("Usuario no encontrado.", model.get("error"));
        }
    }
}
