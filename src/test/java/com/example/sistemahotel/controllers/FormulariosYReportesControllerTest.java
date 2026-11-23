package com.example.sistemahotel.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Reserva;

// Formularios de alta y edición, catálogo de productos, detalle de reserva y los
// indicadores de Reportes calculados con datos (reglas RN23, RN28, RN32 y RN34).
@WithMockUser(username = "elon", roles = "ADMIN")
class FormulariosYReportesControllerTest extends ControladorIntegracionBase {

    private Date diasDesdeHoy(int dias, int hora) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, dias);
        cal.set(Calendar.HOUR_OF_DAY, hora);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Habitacion habitacion(int id, String numero) {
        Categoria categoria = new Categoria();
        categoria.setId(1);
        categoria.setNombre("Habitación Matrimonial");
        categoria.setPrecio(180.0);
        Habitacion h = new Habitacion();
        h.setId(id);
        h.setNumero(numero);
        h.setPiso(1);
        h.setAforo(2);
        h.setCategoria(categoria);
        return h;
    }

    private Registro registro(int id, Habitacion hab, Date checkIn, int noches, double tarifa, double pagado, EstadoRegistro estado) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNombres("Carmen");
        cliente.setApellidos("Herrera");
        cliente.setIdentificacion("7925743" + id);
        Registro r = new Registro();
        r.setId(id);
        r.setCliente(cliente);
        r.setHabitacion(hab);
        r.setCheckIn(checkIn);
        r.setNoches(noches);
        r.setNumHuespedes(1);
        r.setTarifa(tarifa);
        r.setMontoPagado(pagado);
        r.setEstado(estado);
        return r;
    }

    private ProductoConsumo producto(int id, boolean activo) {
        ProductoConsumo p = new ProductoConsumo();
        p.setId(id);
        p.setNombre("Minibar");
        p.setPrecioSugerido(20.0);
        p.setActivo(activo);
        return p;
    }

    @Test
    void reportesSumaElSaldoPendienteDeLasEstadiasActivas() throws Exception {
        Habitacion h = habitacion(1, "101");
        when(habitacionService.listar()).thenReturn(List.of(h));
        when(registroService.listar()).thenReturn(List.of(
            registro(1, h, diasDesdeHoy(0, 10), 2, 360.0, 180.0, EstadoRegistro.ACTIVA),
            registro(2, h, diasDesdeHoy(-10, 12), 2, 400.0, 100.0, EstadoRegistro.ACTIVA),
            registro(3, h, diasDesdeHoy(0, 9), 1, 200.0, 200.0, EstadoRegistro.FINALIZADA)));

        MvcResult resultado = mockMvc.perform(get("/reportes/")).andExpect(status().isOk()).andReturn();

        assertEquals(480.0, (double) resultado.getModelAndView().getModel().get("saldoPendienteTotal"), 0.001);
    }

    @Test
    void losIngresosDeHoyCuentanActivasYFinalizadasPeroNoCanceladas() throws Exception {
        Habitacion h = habitacion(1, "101");
        when(habitacionService.listar()).thenReturn(List.of(h));
        when(registroService.listar()).thenReturn(List.of(
            registro(1, h, diasDesdeHoy(0, 10), 2, 360.0, 180.0, EstadoRegistro.ACTIVA),
            registro(2, h, diasDesdeHoy(0, 9), 1, 200.0, 200.0, EstadoRegistro.FINALIZADA),
            registro(3, h, diasDesdeHoy(0, 8), 1, 180.0, 50.0, EstadoRegistro.CANCELADA)));

        MvcResult resultado = mockMvc.perform(get("/reportes/")).andExpect(status().isOk()).andReturn();

        assertEquals(380.0, (double) resultado.getModelAndView().getModel().get("ingresosHoy"), 0.001);
    }

    @Test
    void reportesCuentaLasEstadiasVencidas() throws Exception {
        Habitacion h = habitacion(1, "101");
        when(habitacionService.listar()).thenReturn(List.of(h));
        when(registroService.listar()).thenReturn(List.of(
            registro(1, h, diasDesdeHoy(-10, 12), 2, 400.0, 100.0, EstadoRegistro.ACTIVA),
            registro(2, h, diasDesdeHoy(1, 12), 2, 360.0, 0.0, EstadoRegistro.ACTIVA)));

        mockMvc.perform(get("/reportes/"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("vencidas", 1L));
    }

    @Test
    void elCalendarioDeOcupacionMuestraLosDiasYLasHabitaciones() throws Exception {
        Habitacion h = habitacion(1, "101");
        when(habitacionService.listar()).thenReturn(List.of(h));
        when(registroService.listar()).thenReturn(List.of(registro(1, h, diasDesdeHoy(1, 12), 3, 540.0, 0.0, EstadoRegistro.ACTIVA)));

        mockMvc.perform(get("/ocupacion/").param("desde", "2027-03-01"))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("dias", "filas", "desdeAnterior", "desdeSiguiente"));
    }

    @Test
    void elFormularioDeNuevoRegistroSeMuestra() throws Exception {
        mockMvc.perform(get("/nuevo/")).andExpect(status().isOk());
    }

    @Test
    void editarUnRegistroInexistenteVuelveALaLista() throws Exception {
        when(registroService.consultarId(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/editar/99/")).andExpect(redirectedUrl("/"));
    }

    @Test
    void elFormularioParaExtenderUnaEstadiaSeMuestra() throws Exception {
        when(registroService.consultarId(6)).thenReturn(Optional.of(registro(6, habitacion(1, "101"), diasDesdeHoy(0, 12), 2, 360.0, 0.0, EstadoRegistro.ACTIVA)));

        mockMvc.perform(get("/extender/6/")).andExpect(status().isOk());
    }

    @Test
    void losFormulariosDeAltaSeMuestran() throws Exception {
        mockMvc.perform(get("/clientes/nuevo/")).andExpect(status().isOk());
        mockMvc.perform(get("/categorias/nuevo/")).andExpect(status().isOk());
        mockMvc.perform(get("/habitaciones/nuevo/")).andExpect(status().isOk());
        mockMvc.perform(get("/productos-consumo/nuevo/")).andExpect(status().isOk());
    }

    @Test
    void editarUnClienteExistenteMuestraSusDatos() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setId(1);
        cliente.setNombres("Carmen");
        cliente.setApellidos("Herrera Rojas");
        cliente.setIdentificacion("79257439");
        when(clienteService.consultarId(1)).thenReturn(Optional.of(cliente));

        mockMvc.perform(get("/clientes/editar/1/"))
            .andExpect(status().isOk())
            // El controlador entrega el cliente envuelto en Optional; la vista lo resuelve igual.
            .andExpect(model().attribute("cliente", Optional.of(cliente)));
    }

    @Test
    void editarUnaCategoriaYUnaHabitacionExistentesMuestraElFormulario() throws Exception {
        Habitacion h = habitacion(1, "101");
        when(categoriaService.consultarId(1)).thenReturn(Optional.of(h.getCategoria()));
        when(habitacionService.consultarId(1)).thenReturn(Optional.of(h));

        mockMvc.perform(get("/categorias/editar/1/")).andExpect(status().isOk());
        mockMvc.perform(get("/habitaciones/editar/1/")).andExpect(status().isOk());
    }

    @Test
    void elCatalogoDeProductosSeMuestra() throws Exception {
        when(productoConsumoService.listar()).thenReturn(List.of(producto(1, true), producto(2, false)));

        mockMvc.perform(get("/productos-consumo/")).andExpect(status().isOk());
    }

    @Test
    void desactivarUnProductoLoSacaDeLosAtajos() throws Exception {
        ProductoConsumo minibar = producto(1, true);
        when(productoConsumoService.consultarId(1)).thenReturn(Optional.of(minibar));

        mockMvc.perform(post("/productos-consumo/desactivar/1/").with(csrf()))
            .andExpect(redirectedUrl("/productos-consumo/"))
            .andExpect(flash().attributeExists("exito"));
        assertFalse(minibar.isActivo());
        verify(productoConsumoService).guardar(minibar);
    }

    @Test
    void reactivarUnProductoLoDevuelveALosAtajos() throws Exception {
        ProductoConsumo minibar = producto(1, false);
        when(productoConsumoService.consultarId(1)).thenReturn(Optional.of(minibar));

        mockMvc.perform(post("/productos-consumo/activar/1/").with(csrf()))
            .andExpect(redirectedUrl("/productos-consumo/"))
            .andExpect(flash().attribute("exito", "Producto reactivado."));
        assertTrue(minibar.isActivo());
    }

    @Test
    void editarUnProductoExistenteMuestraElFormulario() throws Exception {
        when(productoConsumoService.consultarId(1)).thenReturn(Optional.of(producto(1, true)));

        mockMvc.perform(get("/productos-consumo/editar/1/")).andExpect(status().isOk());
    }

    @Test
    void elDetalleDeUnaReservaSeMuestra() throws Exception {
        Habitacion h = habitacion(1, "201");
        Reserva reserva = new Reserva();
        reserva.setId(5);
        Cliente cliente = new Cliente();
        cliente.setNombres("Karen");
        cliente.setApellidos("Aguilar Medina");
        reserva.setCliente(cliente);
        reserva.setRegistros(List.of(registro(1, h, diasDesdeHoy(30, 14), 3, 780.0, 0.0, EstadoRegistro.ACTIVA)));
        when(reservaService.consultarId(5)).thenReturn(Optional.of(reserva));

        mockMvc.perform(get("/reservas/5/")).andExpect(status().isOk());
    }
}
