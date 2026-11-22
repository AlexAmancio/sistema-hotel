package com.example.sistemahotel.controllers;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.sistemahotel.interfaces.ICategoriaService;
import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IComprobanteService;
import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IPasswordResetService;
import com.example.sistemahotel.interfaces.IProductoConsumoService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.interfaces.IReservaService;
import com.example.sistemahotel.servicios.UsuarioService;

// Base común de las pruebas de integración de controladores: levanta el
// contexto de Spring con la seguridad real (rutas, roles y CSRF) y MockMvc, y
// reemplaza los servicios por simulaciones. Como todas las subclases comparten
// el mismo conjunto de simulaciones, Spring arranca el contexto una sola vez.
// UsuarioService se simula por su clase concreta porque SecurityConfig la
// necesita así; la misma simulación atiende a IUsuarioService.
@Tag("integracion")
@SpringBootTest
@AutoConfigureMockMvc
abstract class ControladorIntegracionBase {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected IRegistroService registroService;

    @MockBean
    protected IClienteService clienteService;

    @MockBean
    protected IHabitacionService habitacionService;

    @MockBean
    protected ICategoriaService categoriaService;

    @MockBean
    protected IComprobanteService comprobanteService;

    @MockBean
    protected IConsumoService consumoService;

    @MockBean
    protected IProductoConsumoService productoConsumoService;

    @MockBean
    protected IReservaService reservaService;

    @MockBean
    protected IPasswordResetService passwordResetService;

    @MockBean
    protected UsuarioService usuarioService;
}
