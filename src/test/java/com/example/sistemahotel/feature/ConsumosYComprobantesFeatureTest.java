package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Rutas /consumos/** y /comprobantes/** (HU41 a HU44, HU51 y HU59).
// Reglas RN05 a RN09, RN17 y RN31 a RN33.
class ConsumosYComprobantesFeatureTest extends FeatureTestCase {

    private Usuario recepcion;
    private Habitacion habitacion;
    private Cliente cliente;

    @BeforeEach
    void datos() {
        recepcion = crearRecepcionista("qa_recepcion");
        habitacion = crearHabitacion("Q40", crearCategoria("Categoría QA", 180), 2);
        cliente = crearCliente("Carmen", "Herrera", "88600001");
    }

    private Registro estadia(EstadoRegistro estado, double tarifa, double pagado) {
        return crearEstadia(cliente, habitacion, diasDesdeHoy(-1, 12), 2, tarifa, pagado, estado, recepcion);
    }

    @Test
    void la_pantalla_de_consumos_muestra_la_estadia_y_su_saldo() throws Exception {
        Registro registro = estadia(EstadoRegistro.ACTIVA, 360, 100);
        crearConsumo(registro, "Minibar", 20, null);

        MvcResult respuesta = actingAs(recepcion).get("/consumos/" + registro.getId() + "/");

        assertOk(respuesta);
        assertSee(respuesta, "Minibar");
    }

    @Test
    void agregar_un_consumo_desde_el_catalogo_lo_enlaza_al_producto() throws Exception {
        Registro registro = estadia(EstadoRegistro.ACTIVA, 360, 0);
        ProductoConsumo cafe = crearProducto("Café pasado QA", 8, true);

        MvcResult respuesta = actingAs(recepcion).post("/consumos/agregar/" + registro.getId() + "/",
            "descripcion", "Café pasado QA", "monto", 8, "productoId", cafe.getId());

        assertRedirect(respuesta, "/consumos/" + registro.getId() + "/");
        assertSessionHas(respuesta, "exito", "Consumo agregado correctamente.");
        assertDatabaseHas("consumo", Map.of("registro_id", registro.getId(), "monto", 8.0, "producto_consumo_id", cafe.getId()));
    }

    @Test
    void un_consumo_escrito_a_mano_se_guarda_sin_producto() throws Exception {
        Registro registro = estadia(EstadoRegistro.ACTIVA, 360, 0);

        actingAs(recepcion).post("/consumos/agregar/" + registro.getId() + "/", "descripcion", "Lavandería", "monto", 15);

        assertDatabaseHas("consumo", Map.of("registro_id", registro.getId(), "descripcion", "Lavandería"));
        assertEquals(1, columna("SELECT COUNT(*) FROM consumo WHERE registro_id = ? AND producto_consumo_id IS NULL", Integer.class, registro.getId()));
    }

    @Test
    void un_consumo_sin_descripcion_no_se_guarda() throws Exception {
        Registro registro = estadia(EstadoRegistro.ACTIVA, 360, 0);

        MvcResult respuesta = actingAs(recepcion).post("/consumos/agregar/" + registro.getId() + "/", "descripcion", " ", "monto", 10);

        assertSessionHas(respuesta, "error", "La descripción del consumo es obligatoria.");
        assertDatabaseMissing("consumo", Map.of("registro_id", registro.getId()));
    }

    @Test
    void un_consumo_con_monto_cero_no_se_guarda() throws Exception {
        Registro registro = estadia(EstadoRegistro.ACTIVA, 360, 0);

        MvcResult respuesta = actingAs(recepcion).post("/consumos/agregar/" + registro.getId() + "/", "descripcion", "Agua", "monto", 0);

        assertSessionHas(respuesta, "error", "El monto debe ser mayor a cero.");
        assertDatabaseMissing("consumo", Map.of("registro_id", registro.getId()));
    }

    @Test
    void no_se_agregan_consumos_a_una_estadia_finalizada() throws Exception {
        Registro registro = estadia(EstadoRegistro.FINALIZADA, 360, 360);

        MvcResult respuesta = actingAs(recepcion).post("/consumos/agregar/" + registro.getId() + "/", "descripcion", "Agua", "monto", 5);

        assertSessionHas(respuesta, "error", "ya no está activa");
        assertDatabaseMissing("consumo", Map.of("registro_id", registro.getId()));
    }

    @Test
    void quitar_un_consumo_de_una_estadia_activa_lo_borra() throws Exception {
        Registro registro = estadia(EstadoRegistro.ACTIVA, 360, 0);
        Consumo consumo = crearConsumo(registro, "Agua", 5, null);

        actingAs(recepcion).post("/consumos/eliminar/" + registro.getId() + "/" + consumo.getId() + "/");

        assertDatabaseMissing("consumo", Map.of("id", consumo.getId()));
    }

    @Test
    void no_se_quitan_consumos_de_una_estadia_finalizada() throws Exception {
        Registro registro = estadia(EstadoRegistro.FINALIZADA, 360, 380);
        Consumo consumo = crearConsumo(registro, "Agua", 5, null);

        MvcResult respuesta = actingAs(recepcion).post("/consumos/eliminar/" + registro.getId() + "/" + consumo.getId() + "/");

        assertSessionHas(respuesta, "error", "no se pueden quitar consumos");
        assertDatabaseHas("consumo", Map.of("id", consumo.getId()));
    }

    @Test
    void emitir_el_comprobante_desglosa_el_igv_incluido_en_tarifa_y_consumos() throws Exception {
        Registro registro = estadia(EstadoRegistro.FINALIZADA, 360, 380);
        crearConsumo(registro, "Minibar", 20, null);

        MvcResult respuesta = actingAs(recepcion).post("/comprobantes/emitir/" + registro.getId() + "/");

        assertRedirect(respuesta, "/comprobantes/ver/" + registro.getId() + "/");
        assertDatabaseHas("comprobante", Map.of("registro_id", registro.getId(), "serie", "C001", "total", 380.0, "valor_venta", 322.03, "igv", 57.97));
    }

    @Test
    void emitir_dos_veces_no_duplica_el_comprobante_ni_el_correlativo() throws Exception {
        Registro registro = estadia(EstadoRegistro.FINALIZADA, 360, 360);

        actingAs(recepcion).post("/comprobantes/emitir/" + registro.getId() + "/");
        int correlativo = columna("SELECT correlativo FROM comprobante WHERE registro_id = ?", Integer.class, registro.getId());
        post("/comprobantes/emitir/" + registro.getId() + "/");

        assertDatabaseCount("comprobante", Map.of("registro_id", registro.getId()), 1);
        assertDatabaseHas("comprobante", Map.of("registro_id", registro.getId(), "correlativo", correlativo));
    }

    @Test
    void cada_comprobante_nuevo_toma_el_siguiente_correlativo() throws Exception {
        Registro primero = estadia(EstadoRegistro.FINALIZADA, 360, 360);
        Registro segundo = crearEstadia(crearCliente("Otro", "Huésped", "88600002"), crearHabitacion("Q41", crearCategoria("Otra QA", 100), 1),
            diasDesdeHoy(-1, 12), 1, 100, 100, EstadoRegistro.FINALIZADA, recepcion);

        actingAs(recepcion).post("/comprobantes/emitir/" + primero.getId() + "/");
        post("/comprobantes/emitir/" + segundo.getId() + "/");

        int c1 = columna("SELECT correlativo FROM comprobante WHERE registro_id = ?", Integer.class, primero.getId());
        int c2 = columna("SELECT correlativo FROM comprobante WHERE registro_id = ?", Integer.class, segundo.getId());
        assertEquals(c1 + 1, c2);
    }

    @Test
    void no_se_emite_comprobante_antes_del_check_out() throws Exception {
        Registro registro = estadia(EstadoRegistro.ACTIVA, 360, 0);

        MvcResult respuesta = actingAs(recepcion).post("/comprobantes/emitir/" + registro.getId() + "/");

        assertSessionHas(respuesta, "error", "todavía no tiene check-out hecho");
        assertDatabaseMissing("comprobante", Map.of("registro_id", registro.getId()));
    }

    @Test
    void emitir_para_una_estadia_que_no_existe_avisa_el_error() throws Exception {
        assertSessionHas(actingAs(recepcion).post("/comprobantes/emitir/999999/"), "error", "No se encontró el registro");
    }

    @Test
    void el_comprobante_emitido_se_puede_ver() throws Exception {
        Registro registro = estadia(EstadoRegistro.FINALIZADA, 360, 360);
        actingAs(recepcion).post("/comprobantes/emitir/" + registro.getId() + "/");

        MvcResult respuesta = get("/comprobantes/ver/" + registro.getId() + "/");

        assertOk(respuesta);
        assertSee(respuesta, "C001");
    }

    @Test
    void ver_un_comprobante_no_emitido_vuelve_a_la_lista() throws Exception {
        Registro registro = estadia(EstadoRegistro.FINALIZADA, 360, 360);

        assertRedirect(actingAs(recepcion).get("/comprobantes/ver/" + registro.getId() + "/"), "/");
    }
}
