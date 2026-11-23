package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Rutas de estadías en RegistrosController: check-in, edición, check-out,
// cancelación, no-show, extensión y eliminación (HU31 a HU40 y HU45).
// Reglas RN01 a RN07, RN16, RN22 a RN26 y RN42.
class EstadiasFeatureTest extends FeatureTestCase {

    private Usuario recepcion;
    private Categoria categoria;
    private Habitacion habitacion;
    private Cliente cliente;

    @BeforeEach
    void datos() {
        recepcion = crearRecepcionista("qa_recepcion");
        categoria = crearCategoria("Categoría QA", 180);
        habitacion = crearHabitacion("Q20", categoria, 2);
        cliente = crearCliente("Carmen", "Herrera", "88400001");
    }

    private MvcResult checkIn(Habitacion h, String checkIn, int noches, int huespedes) throws Exception {
        return actingAs(recepcion).post("/guardar/",
            "cliente", cliente.getId(), "habitacion", h.getId(), "checkIn", checkIn, "noches", noches,
            "numHuespedes", huespedes, "tarifa", 180.0 * noches, "montoPagado", 0, "metodoPago", "EFECTIVO");
    }

    private Registro activa() {
        return crearEstadia(cliente, habitacion, diasDesdeHoy(0, 12), 2, 360, 100, EstadoRegistro.ACTIVA, recepcion);
    }

    @Test
    void la_lista_de_estadias_se_muestra_y_se_filtra_por_estado() throws Exception {
        activa();

        assertOk(actingAs(recepcion).get("/"));
        MvcResult filtrada = get("/", "estado", "ACTIVA", "q", "Q20");
        assertOk(filtrada);
        assertSee(filtrada, "Q20");
    }

    @Test
    void el_formulario_de_check_in_se_muestra_con_la_habitacion_preseleccionada() throws Exception {
        MvcResult respuesta = actingAs(recepcion).get("/nuevo/", "habitacion", habitacion.getId(), "checkIn", "2028-03-01T12:00");

        assertOk(respuesta);
        assertTrue(modelo(respuesta, "habitacionPreseleccionada").equals(habitacion.getId()));
    }

    @Test
    void el_check_in_crea_la_estadia_activa_a_nombre_del_usuario_y_ocupa_la_habitacion() throws Exception {
        MvcResult respuesta = checkIn(habitacion, "2028-03-01T12:00", 2, 2);

        assertRedirect(respuesta, "/");
        assertDatabaseHas("registro", Map.of("habitacion_id", habitacion.getId(), "cliente_id", cliente.getId(),
            "usuario_id", recepcion.getId(), "estado", "ACTIVA", "noches", 2));
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "OCUPADA"));
    }

    @Test
    void el_check_in_sin_cliente_no_se_guarda() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/guardar/", "habitacion", habitacion.getId(), "checkIn", "2028-03-01T12:00", "noches", 1, "numHuespedes", 1, "tarifa", 180);

        assertRedirect(respuesta, "/nuevo/");
        assertSessionHas(respuesta, "error", "Elegí un cliente");
        assertDatabaseMissing("registro", Map.of("habitacion_id", habitacion.getId()));
    }

    @Test
    void el_check_in_sin_habitacion_no_se_guarda() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/guardar/", "cliente", cliente.getId(), "checkIn", "2028-03-01T12:00", "noches", 1, "numHuespedes", 1, "tarifa", 180);

        assertSessionHas(respuesta, "error", "Elegí una habitación");
        assertDatabaseMissing("registro", Map.of("cliente_id", cliente.getId()));
    }

    @Test
    void el_check_in_que_supera_el_aforo_no_se_guarda() throws Exception {
        MvcResult respuesta = checkIn(habitacion, "2028-03-01T12:00", 1, 3);

        assertSessionHas(respuesta, "error", "supera el aforo máximo de la habitación Q20 (2 personas)");
        assertDatabaseMissing("registro", Map.of("habitacion_id", habitacion.getId()));
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "DISPONIBLE"));
    }

    @Test
    void el_check_in_que_se_cruza_con_otra_estadia_no_se_guarda() throws Exception {
        crearEstadia(crearCliente("Otro", "Huésped", "88400002"), habitacion, fecha("2028-03-01T12:00"), 3, 540, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = checkIn(habitacion, "2028-03-02T12:00", 1, 1);

        assertSessionHas(respuesta, "error", "ya tiene otra reserva activa que se cruza con esas fechas");
        assertDatabaseMissing("registro", Map.of("cliente_id", cliente.getId()));
    }

    @Test
    void una_estadia_cancelada_no_bloquea_las_mismas_fechas() throws Exception {
        crearEstadia(crearCliente("Otro", "Huésped", "88400003"), habitacion, fecha("2028-03-01T12:00"), 3, 540, 0, EstadoRegistro.CANCELADA, recepcion);

        checkIn(habitacion, "2028-03-02T12:00", 1, 1);

        assertDatabaseHas("registro", Map.of("cliente_id", cliente.getId(), "estado", "ACTIVA"));
    }

    @Test
    void el_formulario_de_edicion_carga_la_estadia() throws Exception {
        Registro registro = activa();

        MvcResult respuesta = actingAs(recepcion).get("/editar/" + registro.getId() + "/");

        assertOk(respuesta);
        assertSee(respuesta, "Editar registro");
    }

    @Test
    void editar_una_estadia_para_registrar_el_pago_actualiza_el_monto() throws Exception {
        Registro registro = activa();

        MvcResult respuesta = actingAs(recepcion).post("/guardar/",
            "id", registro.getId(), "version", registro.getVersion(), "cliente", cliente.getId(), "habitacion", habitacion.getId(),
            "checkIn", "2028-03-01T12:00", "noches", 2, "numHuespedes", 1, "tarifa", 360, "montoPagado", 360, "metodoPago", "TRANSFERENCIA", "estado", "ACTIVA");

        assertRedirect(respuesta, "/");
        assertDatabaseHas("registro", Map.of("id", registro.getId(), "monto_pagado", 360.0, "metodo_pago", "TRANSFERENCIA"));
    }

    @Test
    void cambiar_de_habitacion_libera_la_anterior_y_ocupa_la_nueva() throws Exception {
        Registro registro = activa();
        Habitacion nueva = crearHabitacion("Q21", categoria, 2);

        actingAs(recepcion).post("/guardar/",
            "id", registro.getId(), "version", registro.getVersion(), "cliente", cliente.getId(), "habitacion", nueva.getId(),
            "checkIn", "2029-05-01T12:00", "noches", 2, "numHuespedes", 1, "tarifa", 360, "montoPagado", 100, "metodoPago", "EFECTIVO", "estado", "ACTIVA");

        assertDatabaseHas("registro", Map.of("id", registro.getId(), "habitacion_id", nueva.getId()));
        assertDatabaseHas("habitacion", Map.of("id", nueva.getId(), "estado", "OCUPADA"));
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "DISPONIBLE"));
    }

    @Test
    void editar_con_una_version_vieja_avisa_que_otra_persona_la_modifico() throws Exception {
        Registro registro = activa();
        jdbc.update("UPDATE registro SET version = version + 1 WHERE id = ?", registro.getId());

        MvcResult respuesta = actingAs(recepcion).post("/guardar/",
            "id", registro.getId(), "version", registro.getVersion(), "cliente", cliente.getId(), "habitacion", habitacion.getId(),
            "checkIn", "2028-03-01T12:00", "noches", 2, "numHuespedes", 1, "tarifa", 360, "montoPagado", 360, "metodoPago", "EFECTIVO", "estado", "ACTIVA");

        assertSessionHas(respuesta, "error", "fue modificado por otra persona");
        assertDatabaseHas("registro", Map.of("id", registro.getId(), "monto_pagado", 100.0));
    }

    @Test
    void el_check_out_finaliza_la_estadia_y_manda_la_habitacion_a_limpieza() throws Exception {
        Registro registro = activa();

        MvcResult respuesta = actingAs(recepcion).post("/checkout/" + registro.getId() + "/");

        assertRedirect(respuesta, "/?estado=FINALIZADA");
        assertDatabaseHas("registro", Map.of("id", registro.getId(), "estado", "FINALIZADA"));
        assertTrue(columna("SELECT COUNT(*) FROM registro WHERE id = ? AND check_out_real IS NOT NULL", Integer.class, registro.getId()) == 1);
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "LIMPIEZA"));
    }

    @Test
    void cancelar_una_estadia_ocupada_la_manda_a_limpieza() throws Exception {
        Registro registro = activa();

        actingAs(recepcion).post("/cancelar/" + registro.getId() + "/");

        assertDatabaseHas("registro", Map.of("id", registro.getId(), "estado", "CANCELADA"));
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "LIMPIEZA"));
    }

    @Test
    void no_se_cancela_una_estadia_con_consumos() throws Exception {
        Registro registro = activa();
        crearConsumo(registro, "Minibar", 15, null);

        MvcResult respuesta = actingAs(recepcion).post("/cancelar/" + registro.getId() + "/");

        assertSessionHas(respuesta, "error", "No se puede cancelar: ya tiene consumos cargados");
        assertDatabaseHas("registro", Map.of("id", registro.getId(), "estado", "ACTIVA"));
    }

    @Test
    void el_no_show_libera_la_habitacion_sin_pasar_por_limpieza() throws Exception {
        Registro registro = activa();

        actingAs(recepcion).post("/no-show/" + registro.getId() + "/");

        assertDatabaseHas("registro", Map.of("id", registro.getId(), "estado", "NO_SHOW"));
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "DISPONIBLE"));
    }

    @Test
    void no_se_marca_no_show_una_estadia_con_consumos() throws Exception {
        Registro registro = activa();
        crearConsumo(registro, "Desayuno", 25, null);

        MvcResult respuesta = actingAs(recepcion).post("/no-show/" + registro.getId() + "/");

        assertSessionHas(respuesta, "error", "No se puede marcar como no-show");
        assertDatabaseHas("registro", Map.of("id", registro.getId(), "estado", "ACTIVA"));
    }

    @Test
    void el_formulario_de_extension_se_muestra() throws Exception {
        assertOk(actingAs(recepcion).get("/extender/" + activa().getId() + "/"));
    }

    @Test
    void extender_la_estadia_recalcula_la_tarifa_con_las_noches_nuevas() throws Exception {
        Registro registro = activa();

        MvcResult respuesta = actingAs(recepcion).post("/extender/" + registro.getId() + "/", "noches", 4);

        assertRedirect(respuesta, "/");
        assertDatabaseHas("registro", Map.of("id", registro.getId(), "noches", 4, "tarifa", 720.0));
    }

    @Test
    void no_se_extiende_si_choca_con_la_siguiente_estadia() throws Exception {
        Registro registro = activa();
        crearEstadia(crearCliente("Otro", "Huésped", "88400004"), habitacion, diasDesdeHoy(2, 12), 2, 360, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).post("/extender/" + registro.getId() + "/", "noches", 4);

        assertRedirect(respuesta, "/extender/" + registro.getId() + "/");
        assertSessionHas(respuesta, "error", "No se pudo extender");
        assertDatabaseHas("registro", Map.of("id", registro.getId(), "noches", 2));
    }

    @Test
    void eliminar_una_estadia_sin_consumos_ni_comprobante_la_borra() throws Exception {
        Registro registro = activa();

        actingAs(recepcion).post("/eliminar/" + registro.getId() + "/");

        assertDatabaseMissing("registro", Map.of("id", registro.getId()));
    }

    @Test
    void no_se_elimina_una_estadia_con_consumos() throws Exception {
        Registro registro = activa();
        crearConsumo(registro, "Minibar", 15, null);

        MvcResult respuesta = actingAs(recepcion).post("/eliminar/" + registro.getId() + "/");

        assertSessionHas(respuesta, "error", "tiene consumos cargados");
        assertDatabaseHas("registro", Map.of("id", registro.getId()));
    }

    @Test
    void no_se_elimina_una_estadia_con_comprobante() throws Exception {
        Registro registro = crearEstadia(cliente, habitacion, diasDesdeHoy(-2, 12), 1, 180, 180, EstadoRegistro.FINALIZADA, recepcion);
        actingAs(recepcion).post("/comprobantes/emitir/" + registro.getId() + "/");

        MvcResult respuesta = post("/eliminar/" + registro.getId() + "/");

        assertSessionHas(respuesta, "error", "ya tiene un comprobante emitido");
        assertDatabaseHas("registro", Map.of("id", registro.getId()));
    }

    @Test
    void la_vista_de_hoy_separa_llegadas_salidas_y_vencidas() throws Exception {
        crearEstadia(cliente, habitacion, diasDesdeHoy(-3, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).get("/hoy/");

        assertOk(respuesta);
        assertTrue(((java.util.List<?>) modelo(respuesta, "vencidas")).stream()
            .anyMatch(r -> ((Registro) r).getHabitacion().getNumero().equals("Q20")));
    }

    @Test
    void exportar_descarga_un_csv_con_las_estadias() throws Exception {
        activa();

        MvcResult respuesta = actingAs(recepcion).get("/exportar/", "q", "Q20");

        assertTrue(respuesta.getResponse().getContentType().startsWith("text/csv"));
        assertTrue(respuesta.getResponse().getHeader("Content-Disposition").contains("registros.csv"));
        assertSee(respuesta, "Carmen Herrera");
    }

    @Test
    void una_habitacion_ocupada_no_cambia_si_se_edita_una_estadia_finalizada() throws Exception {
        Registro finalizada = crearEstadia(cliente, habitacion, diasDesdeHoy(-5, 12), 1, 180, 180, EstadoRegistro.FINALIZADA, recepcion);
        habitacion.setEstado(EstadoHabitacion.DISPONIBLE);
        habitaciones.save(habitacion);

        actingAs(recepcion).post("/guardar/",
            "id", finalizada.getId(), "version", finalizada.getVersion(), "cliente", cliente.getId(), "habitacion", habitacion.getId(),
            "checkIn", "2026-01-01T12:00", "noches", 1, "numHuespedes", 1, "tarifa", 180, "montoPagado", 180, "metodoPago", "EFECTIVO",
            "estado", "FINALIZADA", "observaciones", "Factura enviada");

        assertDatabaseHas("registro", Map.of("id", finalizada.getId(), "observaciones", "Factura enviada"));
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "DISPONIBLE"));
    }
}
