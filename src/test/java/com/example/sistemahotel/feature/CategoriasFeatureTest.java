package com.example.sistemahotel.feature;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Categoria;

// Rutas /categorias/** (HU20 a HU23). Reglas RN10, RN14 y RN15.
class CategoriasFeatureTest extends FeatureTestCase {

    @Test
    void la_lista_de_categorias_muestra_nombre_y_tarifa() throws Exception {
        crearCategoria("Categoría QA", 199);

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).get("/categorias/");

        assertOk(respuesta);
        assertSee(respuesta, "Categoría QA");
    }

    @Test
    void los_formularios_de_alta_y_edicion_se_muestran() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 199);

        assertOk(actingAs(crearAdmin("qa_admin")).get("/categorias/nuevo/"));
        assertOk(get("/categorias/editar/" + categoria.getId() + "/"));
    }

    @Test
    void crear_una_categoria_con_temporada_alta() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/categorias/guardar/",
            "nombre", "Domo QA", "precio", "300", "descripcion", "Domo de prueba",
            "precioTemporadaAlta", "380", "temporadaAltaInicio", "2027-12-20", "temporadaAltaFin", "2028-01-05");

        assertRedirect(respuesta, "/categorias/");
        assertSessionHas(respuesta, "exito", "Categoría creada correctamente.");
        assertDatabaseHas("categoria", Map.of("nombre", "Domo QA", "precio", 300.0, "precio_temporada_alta", 380.0));
    }

    @Test
    void una_categoria_con_precio_cero_no_se_guarda() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/categorias/guardar/", "nombre", "Gratis QA", "precio", "0");

        assertSessionHas(respuesta, "error", "El precio debe ser mayor a cero.");
        assertDatabaseMissing("categoria", Map.of("nombre", "Gratis QA"));
    }

    @Test
    void una_temporada_alta_que_termina_antes_de_empezar_no_se_guarda() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/categorias/guardar/",
            "nombre", "Temporada QA", "precio", "100", "precioTemporadaAlta", "150", "temporadaAltaInicio", "2027-03-20", "temporadaAltaFin", "2027-03-10");

        assertSessionHas(respuesta, "error", "debe ser posterior a la de inicio");
        assertDatabaseMissing("categoria", Map.of("nombre", "Temporada QA"));
    }

    @Test
    void no_se_repite_el_nombre_de_una_categoria() throws Exception {
        crearCategoria("Categoría QA", 199);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/categorias/guardar/", "nombre", "Categoría QA", "precio", "250");

        assertSessionHas(respuesta, "error", "Nombre repetido.");
        assertDatabaseCount("categoria", Map.of("nombre", "Categoría QA"), 1);
    }

    @Test
    void editar_una_categoria_actualiza_su_precio() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 199);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/categorias/guardar/", "id", categoria.getId(), "nombre", "Categoría QA", "precio", "210");

        assertSessionHas(respuesta, "exito", "Categoría actualizada correctamente.");
        assertDatabaseHas("categoria", Map.of("id", categoria.getId(), "precio", 210.0));
    }

    @Test
    void eliminar_una_categoria_sin_habitaciones_la_borra() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 199);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/categorias/eliminar/" + categoria.getId() + "/");

        assertSessionHas(respuesta, "exito", "Categoría eliminada correctamente.");
        assertDatabaseMissing("categoria", Map.of("id", categoria.getId()));
    }

    @Test
    void no_se_elimina_una_categoria_que_usan_habitaciones() throws Exception {
        Categoria categoria = crearCategoria("Categoría QA", 199);
        crearHabitacion("Q01", categoria, 2);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/categorias/eliminar/" + categoria.getId() + "/");

        assertSessionHas(respuesta, "error", "hay habitaciones que usan esta categoría");
        assertDatabaseHas("categoria", Map.of("id", categoria.getId()));
    }
}
