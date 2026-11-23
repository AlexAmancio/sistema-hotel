package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.modelos.Usuario;

// Flujos de negocio completos de punta a punta, cada uno en una sola prueba
// (como PermissionWorkflowTest en Laravel): encadenan varias rutas y verifican
// el estado de la base después de cada paso.
class FlujosCompletosFeatureTest extends FeatureTestCase {

    private Usuario admin;
    private Usuario recepcion;
    private Categoria categoria;
    private Cliente cliente;

    @BeforeEach
    void datos() {
        admin = crearAdmin("qa_admin");
        recepcion = crearRecepcionista("qa_recepcion");
        categoria = crearCategoria("Categoría Flujo QA", 200);
        cliente = crearCliente("Familia", "Flujo", "88900001");
    }

    private int registroDe(int habitacionId) {
        return columna("SELECT MAX(id) FROM registro WHERE habitacion_id = ?", Integer.class, habitacionId);
    }

    private String estadoHabitacion(int habitacionId) {
        return columna("SELECT estado FROM habitacion WHERE id = ?", String.class, habitacionId);
    }

    private void checkIn(int habitacionId, String fecha, int noches, double tarifa) throws Exception {
        actingAs(recepcion).post("/guardar/", "cliente", cliente.getId(), "habitacion", habitacionId, "checkIn", fecha,
            "noches", noches, "numHuespedes", 1, "tarifa", tarifa, "montoPagado", 0, "metodoPago", "EFECTIVO");
    }

    @Test
    void reserva_de_dos_habitaciones_desde_la_reserva_hasta_que_vuelven_a_estar_disponibles() throws Exception {
        int a = crearHabitacion("QF1", categoria, 2).getId();
        int b = crearHabitacion("QF2", categoria, 3).getId();
        ProductoConsumo trucha = crearProducto("Trucha frita QA", 35, true);

        // 1. Reserva de las dos habitaciones por 2 noches (200 x 2 = 400 cada una).
        actingAs(recepcion).post("/reservas/guardar/", "cliente", cliente.getId(), "checkIn", "2028-06-10T12:00", "noches", 2,
            "habitaciones", List.of(a, b), "huespedes_" + a, 2, "huespedes_" + b, 3);
        int ra = registroDe(a);
        int rb = registroDe(b);
        assertDatabaseHas("registro", Map.of("id", ra, "estado", "ACTIVA", "tarifa", 400.0));
        assertDatabaseHas("registro", Map.of("id", rb, "estado", "ACTIVA", "tarifa", 400.0));
        assertEquals("OCUPADA", estadoHabitacion(a));

        // 2. Consumos en la primera habitación: uno del catálogo y uno a mano.
        post("/consumos/agregar/" + ra + "/", "descripcion", "Trucha frita QA", "monto", 35, "productoId", trucha.getId());
        post("/consumos/agregar/" + ra + "/", "descripcion", "Lavandería", "monto", 15);
        assertDatabaseCount("consumo", Map.of("registro_id", ra), 2);

        // 3. Pago total de la primera (400 + 50 = 450).
        long version = columna("SELECT version FROM registro WHERE id = ?", Long.class, ra);
        post("/guardar/", "id", ra, "version", version, "cliente", cliente.getId(), "habitacion", a, "checkIn", "2028-06-10T12:00",
            "noches", 2, "numHuespedes", 2, "tarifa", 400, "montoPagado", 450, "metodoPago", "TARJETA", "estado", "ACTIVA");
        assertDatabaseHas("registro", Map.of("id", ra, "monto_pagado", 450.0, "metodo_pago", "TARJETA"));

        // 4. Antes del check-out no se puede emitir comprobante.
        assertSessionHas(post("/comprobantes/emitir/" + ra + "/"), "error", "todavía no tiene check-out hecho");

        // 5. Check-out de ambas: finalizadas y habitaciones a limpieza.
        post("/checkout/" + ra + "/");
        post("/checkout/" + rb + "/");
        assertDatabaseHas("registro", Map.of("id", ra, "estado", "FINALIZADA"));
        assertEquals("LIMPIEZA", estadoHabitacion(a));
        assertEquals("LIMPIEZA", estadoHabitacion(b));

        // 6. Ya no se aceptan consumos.
        assertSessionHas(post("/consumos/agregar/" + ra + "/", "descripcion", "Agua", "monto", 3), "error", "ya no está activa");

        // 7. Comprobante con IGV incluido: 450 / 1,18 = 381,36 + 68,64.
        assertRedirect(post("/comprobantes/emitir/" + ra + "/"), "/comprobantes/ver/" + ra + "/");
        assertDatabaseHas("comprobante", Map.of("registro_id", ra, "total", 450.0, "valor_venta", 381.36, "igv", 68.64));

        // 8. Mientras está en limpieza no se ofrece; al marcarla disponible, sí.
        MvcResult antes = get("/habitaciones/disponibles", "checkIn", "2028-07-01T12:00", "noches", 1);
        assertDontSee(antes, "\"numero\":\"QF1\"");
        post("/habitaciones/marcar-disponible/" + a + "/");
        assertEquals("DISPONIBLE", estadoHabitacion(a));
        assertSee(get("/habitaciones/disponibles", "checkIn", "2028-07-01T12:00", "noches", 1), "\"numero\":\"QF1\"");
    }

    @Test
    void check_in_extension_consumo_check_out_y_comprobante_con_la_tarifa_extendida() throws Exception {
        int h = crearHabitacion("QF3", categoria, 2).getId();

        checkIn(h, "2028-08-01T12:00", 1, 200);
        int r = registroDe(h);

        assertRedirect(actingAs(recepcion).post("/extender/" + r + "/", "noches", 3), "/");
        assertDatabaseHas("registro", Map.of("id", r, "noches", 3, "tarifa", 600.0));

        post("/consumos/agregar/" + r + "/", "descripcion", "Desayuno", "monto", 30);
        post("/checkout/" + r + "/");
        post("/comprobantes/emitir/" + r + "/");

        assertDatabaseHas("comprobante", Map.of("registro_id", r, "total", 630.0));
        assertSee(get("/comprobantes/ver/" + r + "/"), "C001");
    }

    @Test
    void una_estadia_cancelada_no_cobra_no_admite_consumos_ni_comprobante_y_no_suma_ingresos() throws Exception {
        int h = crearHabitacion("QF4", categoria, 2).getId();
        double ingresosAntes = (double) modelo(actingAs(recepcion).get("/reportes/"), "ingresosHoy");

        actingAs(recepcion).post("/guardar/", "cliente", cliente.getId(), "habitacion", h, "checkIn", hoyAlMediodia(),
            "noches", 1, "numHuespedes", 1, "tarifa", 200, "montoPagado", 80, "metodoPago", "EFECTIVO");
        int r = registroDe(h);
        assertEquals(ingresosAntes + 80, (double) modelo(get("/reportes/"), "ingresosHoy"), 0.001);

        post("/cancelar/" + r + "/");

        assertDatabaseHas("registro", Map.of("id", r, "estado", "CANCELADA"));
        assertEquals("LIMPIEZA", estadoHabitacion(h));
        assertSessionHas(post("/consumos/agregar/" + r + "/", "descripcion", "Agua", "monto", 3), "error", "ya no está activa");
        assertSessionHas(post("/comprobantes/emitir/" + r + "/"), "error", "todavía no tiene check-out hecho");
        assertEquals(ingresosAntes, (double) modelo(get("/reportes/"), "ingresosHoy"), 0.001);
    }

    @Test
    void un_no_show_libera_la_habitacion_y_permite_otro_check_in_en_las_mismas_fechas() throws Exception {
        int h = crearHabitacion("QF5", categoria, 2).getId();

        checkIn(h, "2028-09-01T12:00", 2, 400);
        int primero = registroDe(h);
        post("/no-show/" + primero + "/");

        assertDatabaseHas("registro", Map.of("id", primero, "estado", "NO_SHOW"));
        assertEquals("DISPONIBLE", estadoHabitacion(h));

        Cliente otro = crearCliente("Otro", "Huésped", "88900002");
        actingAs(recepcion).post("/guardar/", "cliente", otro.getId(), "habitacion", h, "checkIn", "2028-09-01T12:00",
            "noches", 2, "numHuespedes", 1, "tarifa", 400, "montoPagado", 0, "metodoPago", "EFECTIVO");

        assertDatabaseHas("registro", Map.of("habitacion_id", h, "cliente_id", otro.getId(), "estado", "ACTIVA"));
        assertEquals("OCUPADA", estadoHabitacion(h));
    }

    @Test
    void recuperar_la_contrasena_desde_el_correo_hasta_iniciar_sesion_con_la_nueva() throws Exception {
        // 1. Pide el enlace y se captura el correo enviado.
        comoInvitado().post("/login/olvide-contrasena", "correo", "qa_recepcion@tunki.test");
        ArgumentCaptor<SimpleMailMessage> correo = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(correo.capture());
        assertEquals("qa_recepcion@tunki.test", correo.getValue().getTo()[0]);
        Matcher m = Pattern.compile("token=([\\w-]+)").matcher(correo.getValue().getText());
        assertTrue(m.find(), "El correo no trae el enlace");
        String token = m.group(1);

        // 2. Abre el enlace y cambia la contraseña.
        assertEquals("formulario", modelo(get("/login/restablecer", "token", token), "estado"));
        assertEquals("exito", modelo(post("/login/restablecer", "token", token, "nuevaContrasena", "Recuperada2026"), "estado"));

        // 3. El enlace ya no sirve una segunda vez.
        assertEquals("invalido", modelo(get("/login/restablecer", "token", token), "estado"));

        // 4. Entra con la nueva y ya no con la anterior.
        assertRedirect(desdeLaIp("10.71.0.1").post("/login", "username", "qa_recepcion", "password", "Recuperada2026"), "/");
        String fallo = URLDecoder.decode(desdeLaIp("10.71.0.2").post("/login", "username", "qa_recepcion", "password", "Secreto2026")
            .getResponse().getRedirectedUrl(), StandardCharsets.UTF_8);
        assertTrue(fallo.startsWith("/login?error"));
    }

    @Test
    void el_administrador_da_de_alta_a_una_recepcionista_que_entra_con_permisos_limitados_y_luego_la_desactiva() throws Exception {
        actingAs(admin).post("/usuarios/guardar/", "username", "qa_nueva", "nombres", "rosa", "apellidos", "vargas",
            "correo", "qa_nueva@tunki.test", "contrasena", "Inicial2026", "activo", "true");
        assertDatabaseHas("usuario", Map.of("username", "qa_nueva", "nombres", "Rosa", "es_admin", false, "activo", true));

        assertRedirect(comoInvitado().desdeLaIp("10.71.0.3").post("/login", "username", "qa_nueva", "password", "Inicial2026"), "/");

        Usuario nueva = usuarios.findByUsername("qa_nueva").orElseThrow();
        assertOk(actingAs(nueva).get("/usuarios/"));
        assertForbidden(get("/usuarios/nuevo/"));
        assertForbidden(post("/usuarios/guardar/", "username", "qa_pirata", "nombres", "X", "apellidos", "Y", "contrasena", "Clave2026"));
        assertDatabaseMissing("usuario", Map.of("username", "qa_pirata"));

        actingAs(admin).post("/usuarios/desactivar/" + nueva.getId() + "/");
        String fallo = URLDecoder.decode(comoInvitado().desdeLaIp("10.71.0.4").post("/login", "username", "qa_nueva", "password", "Inicial2026")
            .getResponse().getRedirectedUrl(), StandardCharsets.UTF_8);
        assertTrue(fallo.startsWith("/login?error"));
    }

    @Test
    void un_producto_del_catalogo_se_cobra_ya_no_se_puede_borrar_y_al_desactivarlo_sale_de_los_atajos() throws Exception {
        int h = crearHabitacion("QF6", categoria, 2).getId();

        actingAs(admin).post("/productos-consumo/guardar/", "nombre", "Jugo de cocona QA", "precioSugerido", "12", "activo", "true");
        int producto = columna("SELECT id FROM producto_consumo WHERE nombre = ?", Integer.class, "Jugo de cocona QA");

        checkIn(h, "2028-10-01T12:00", 1, 200);
        int r = registroDe(h);
        assertSee(actingAs(recepcion).get("/consumos/" + r + "/"), "Jugo de cocona QA");
        post("/consumos/agregar/" + r + "/", "descripcion", "Jugo de cocona QA", "monto", 12, "productoId", producto);

        assertSessionHas(actingAs(admin).post("/productos-consumo/eliminar/" + producto + "/"), "error", "Desactivalo en vez de eliminarlo");
        post("/productos-consumo/desactivar/" + producto + "/");

        MvcResult consumos = actingAs(recepcion).get("/consumos/" + r + "/");
        assertTrue(((List<?>) modelo(consumos, "listaProductos")).stream()
            .noneMatch(p -> ((ProductoConsumo) p).getId() == producto));
        assertDatabaseHas("consumo", Map.of("registro_id", r, "producto_consumo_id", producto));
    }

    private String hoyAlMediodia() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(diasDesdeHoy(0, 12));
    }
}
