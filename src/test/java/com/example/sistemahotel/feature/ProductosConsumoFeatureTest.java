package com.example.sistemahotel.feature;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Rutas /productos-consumo/** (HU52 a HU58). Reglas RN10, RN28 y RN34.
class ProductosConsumoFeatureTest extends FeatureTestCase {

    @Test
    void cualquier_usuario_ve_el_catalogo() throws Exception {
        crearProducto("Café pasado QA", 8, true);

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).get("/productos-consumo/");

        assertOk(respuesta);
        assertSee(respuesta, "Café pasado QA");
    }

    @Test
    void los_formularios_de_alta_y_edicion_se_muestran_al_administrador() throws Exception {
        ProductoConsumo producto = crearProducto("Café pasado QA", 8, true);

        assertOk(actingAs(crearAdmin("qa_admin")).get("/productos-consumo/nuevo/"));
        assertOk(get("/productos-consumo/editar/" + producto.getId() + "/"));
    }

    @Test
    void el_administrador_crea_un_producto() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/productos-consumo/guardar/", "nombre", "Trucha QA", "precioSugerido", "35", "activo", "true");

        assertRedirect(respuesta, "/productos-consumo/");
        assertSessionHas(respuesta, "exito", "Producto creado correctamente.");
        assertDatabaseHas("producto_consumo", Map.of("nombre", "Trucha QA", "precio_sugerido", 35.0, "activo", true));
    }

    @Test
    void un_producto_sin_nombre_no_se_guarda() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/productos-consumo/guardar/", "nombre", " ", "precioSugerido", "10");

        assertRedirect(respuesta, "/productos-consumo/nuevo/");
        assertSessionHas(respuesta, "error", "El nombre es obligatorio.");
    }

    @Test
    void un_producto_con_precio_negativo_no_se_guarda() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/productos-consumo/guardar/", "nombre", "Negativo QA", "precioSugerido", "-1");

        assertSessionHas(respuesta, "error", "no puede ser negativo");
        assertDatabaseMissing("producto_consumo", Map.of("nombre", "Negativo QA"));
    }

    @Test
    void editar_un_producto_actualiza_su_precio() throws Exception {
        ProductoConsumo producto = crearProducto("Café pasado QA", 8, true);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/productos-consumo/guardar/", "id", producto.getId(), "nombre", "Café pasado QA", "precioSugerido", "9", "activo", "true");

        assertSessionHas(respuesta, "exito", "Producto actualizado correctamente.");
        assertDatabaseHas("producto_consumo", Map.of("id", producto.getId(), "precio_sugerido", 9.0));
    }

    @Test
    void desactivar_y_reactivar_un_producto() throws Exception {
        Usuario admin = crearAdmin("qa_admin");
        ProductoConsumo producto = crearProducto("Café pasado QA", 8, true);

        assertSessionHas(actingAs(admin).post("/productos-consumo/desactivar/" + producto.getId() + "/"), "exito", "Producto desactivado");
        assertDatabaseHas("producto_consumo", Map.of("id", producto.getId(), "activo", false));

        assertSessionHas(post("/productos-consumo/activar/" + producto.getId() + "/"), "exito", "Producto reactivado.");
        assertDatabaseHas("producto_consumo", Map.of("id", producto.getId(), "activo", true));
    }

    @Test
    void eliminar_un_producto_sin_consumos_lo_borra() throws Exception {
        ProductoConsumo producto = crearProducto("Café pasado QA", 8, true);

        assertSessionHas(actingAs(crearAdmin("qa_admin")).post("/productos-consumo/eliminar/" + producto.getId() + "/"), "exito", "Producto eliminado correctamente.");
        assertDatabaseMissing("producto_consumo", Map.of("id", producto.getId()));
    }

    @Test
    void no_se_elimina_un_producto_que_ya_se_cobro() throws Exception {
        Usuario admin = crearAdmin("qa_admin");
        ProductoConsumo producto = crearProducto("Café pasado QA", 8, true);
        Registro registro = crearEstadia(crearCliente("Carmen", "Herrera", "88700001"), crearHabitacion("Q50", crearCategoria("Categoría QA", 180), 2),
            diasDesdeHoy(0, 12), 1, 180, 0, EstadoRegistro.ACTIVA, admin);
        crearConsumo(registro, "Café pasado QA", 8, producto);

        MvcResult respuesta = actingAs(admin).post("/productos-consumo/eliminar/" + producto.getId() + "/");

        assertSessionHas(respuesta, "error", "Desactivalo en vez de eliminarlo");
        assertDatabaseHas("producto_consumo", Map.of("id", producto.getId()));
    }

    @Test
    void recepcion_no_puede_crear_ni_cambiar_productos() throws Exception {
        ProductoConsumo producto = crearProducto("Café pasado QA", 8, true);
        Usuario recepcion = crearRecepcionista("qa_recepcion");

        assertForbidden(actingAs(recepcion).get("/productos-consumo/nuevo/"));
        assertForbidden(post("/productos-consumo/guardar/", "nombre", "Pirata QA", "precioSugerido", "1"));
        assertForbidden(post("/productos-consumo/desactivar/" + producto.getId() + "/"));
        assertDatabaseMissing("producto_consumo", Map.of("nombre", "Pirata QA"));
        assertDatabaseHas("producto_consumo", Map.of("id", producto.getId(), "activo", true));
    }
}
