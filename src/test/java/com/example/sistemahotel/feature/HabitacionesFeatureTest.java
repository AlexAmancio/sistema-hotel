package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Usuario;

// Rutas /habitaciones/** (HU24 a HU30). Reglas RN01, RN04, RN10, RN14 y RN18.
class HabitacionesFeatureTest extends FeatureTestCase {

    @Test
    void la_lista_muestra_las_habitaciones_agrupadas_por_piso() throws Exception {
        crearHabitacion("Q01", crearCategoria("Categoría QA", 180), 2);

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).get("/habitaciones/");

        assertOk(respuesta);
        assertSee(respuesta, "Q01");
        assertSee(respuesta, "Piso 9");
    }

    @Test
    void los_formularios_de_alta_y_edicion_se_muestran() throws Exception {
        Habitacion habitacion = crearHabitacion("Q01", crearCategoria("Categoría QA", 180), 2);

        assertOk(actingAs(crearAdmin("qa_admin")).get("/habitaciones/nuevo/"));
        assertOk(get("/habitaciones/editar/" + habitacion.getId() + "/"));
    }

    @Test
    void crear_una_habitacion_la_guarda_disponible() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 180);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/habitaciones/guardar/",
            "numero", "Q02", "piso", "9", "aforo", "3", "categoria", categoria.getId(), "estado", "DISPONIBLE");

        assertRedirect(respuesta, "/habitaciones/");
        assertSessionHas(respuesta, "exito", "Habitación creada correctamente.");
        assertDatabaseHas("habitacion", Map.of("numero", "Q02", "aforo", 3, "categoria_id", categoria.getId(), "estado", "DISPONIBLE"));
    }

    @Test
    void no_se_repite_el_numero_de_habitacion_al_crear() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 180);
        crearHabitacion("Q03", categoria, 2);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/habitaciones/guardar/",
            "numero", "Q03", "piso", "9", "aforo", "2", "categoria", categoria.getId(), "estado", "DISPONIBLE");

        assertSessionHas(respuesta, "error", "N° de habitación repetido.");
        assertDatabaseCount("habitacion", Map.of("numero", "Q03"), 1);
    }

    @Test
    void no_se_repite_el_numero_de_habitacion_al_editar() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 180);
        crearHabitacion("Q04", categoria, 2);
        Habitacion otra = crearHabitacion("Q05", categoria, 2);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/habitaciones/guardar/",
            "id", otra.getId(), "version", otra.getVersion(), "numero", "Q04", "piso", "9", "aforo", "2", "categoria", categoria.getId(), "estado", "DISPONIBLE");

        assertSessionHas(respuesta, "error", "N° de habitación repetido.");
        assertDatabaseHas("habitacion", Map.of("id", otra.getId(), "numero", "Q05"));
    }

    @Test
    void editar_una_habitacion_actualiza_su_aforo() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 180);
        Habitacion habitacion = crearHabitacion("Q06", categoria, 2);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/habitaciones/guardar/",
            "id", habitacion.getId(), "version", habitacion.getVersion(), "numero", "Q06", "piso", "9", "aforo", "4", "categoria", categoria.getId(), "estado", "DISPONIBLE");

        assertSessionHas(respuesta, "exito", "Habitación actualizada correctamente.");
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "aforo", 4));
    }

    @Test
    void eliminar_una_habitacion_sin_estadias_la_borra() throws Exception {
        Habitacion habitacion = crearHabitacion("Q07", crearCategoria("Categoría QA", 180), 2);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/habitaciones/eliminar/" + habitacion.getId() + "/");

        assertSessionHas(respuesta, "exito", "Habitación eliminada correctamente.");
        assertDatabaseMissing("habitacion", Map.of("id", habitacion.getId()));
    }

    @Test
    void no_se_elimina_una_habitacion_con_estadias() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");
        Habitacion habitacion = crearHabitacion("Q08", crearCategoria("Categoría QA", 180), 2);
        crearEstadia(crearCliente("Carmen", "Herrera", "88300001"), habitacion, diasDesdeHoy(0, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/habitaciones/eliminar/" + habitacion.getId() + "/");

        assertSessionHas(respuesta, "error", "tiene registros asociados");
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId()));
    }

    @Test
    void marcar_disponible_una_habitacion_en_limpieza() throws Exception {
        Habitacion habitacion = crearHabitacion("Q09", crearCategoria("Categoría QA", 180), 2, EstadoHabitacion.LIMPIEZA);

        assertOk(actingAs(crearRecepcionista("qa_recepcion")).post("/habitaciones/marcar-disponible/" + habitacion.getId() + "/"));
        assertDatabaseHas("habitacion", Map.of("id", habitacion.getId(), "estado", "DISPONIBLE"));
    }

    @Test
    void las_disponibles_excluyen_las_que_estan_en_limpieza_o_tienen_estadia_en_esas_fechas() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");
        Categoria categoria = crearCategoria("Categoría QA", 180);
        crearHabitacion("Q10", categoria, 2);
        crearHabitacion("Q11", categoria, 2, EstadoHabitacion.LIMPIEZA);
        Habitacion ocupada = crearHabitacion("Q12", categoria, 2);
        Cliente cliente = crearCliente("Carmen", "Herrera", "88300002");
        crearEstadia(cliente, ocupada, fecha("2028-02-01T12:00"), 3, 540, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).get("/habitaciones/disponibles", "checkIn", "2028-02-02T12:00", "noches", 1);

        assertOk(respuesta);
        assertSee(respuesta, "\"numero\":\"Q10\"");
        assertDontSee(respuesta, "\"numero\":\"Q11\"");
        assertDontSee(respuesta, "\"numero\":\"Q12\"");
    }

    @Test
    void la_tarifa_se_calcula_por_noches_y_por_temporada() throws Exception {
        Categoria categoria = crearCategoriaConTemporada("Domo QA", 300, 380, "2027-12-20", "2028-01-05");
        Habitacion habitacion = crearHabitacion("Q13", categoria, 4);
        Usuario recepcion = crearRecepcionista("qa_recepcion");

        MvcResult fuera = actingAs(recepcion).get("/habitaciones/calcular-tarifa/" + habitacion.getId(), "noches", 2, "checkIn", "2027-11-10T12:00");
        MvcResult dentro = get("/habitaciones/calcular-tarifa/" + habitacion.getId(), "noches", 2, "checkIn", "2027-12-26T12:00");

        assertSee(fuera, "600.0");
        assertSee(dentro, "760.0");
    }

    @Test
    void la_tarifa_de_una_habitacion_que_no_existe_devuelve_404() throws Exception {
        assertStatus(actingAs(crearRecepcionista("qa_recepcion")).get("/habitaciones/calcular-tarifa/999999", "noches", 1), 404);
        assertTrue(true);
    }
}
