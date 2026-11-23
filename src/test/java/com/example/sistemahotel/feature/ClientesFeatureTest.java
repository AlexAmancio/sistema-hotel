package com.example.sistemahotel.feature;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Usuario;

// Rutas /clientes/** (HU14 a HU19). Reglas RN10, RN14, RN29, RN40 y RN41.
class ClientesFeatureTest extends FeatureTestCase {

    @Test
    void la_lista_de_clientes_se_muestra_paginada() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).get("/clientes/", "pagina", 0);

        assertOk(respuesta);
        assertSee(respuesta, "Agregar cliente");
    }

    @Test
    void el_formulario_de_nuevo_cliente_se_muestra() throws Exception {
        assertOk(actingAs(crearRecepcionista("qa_recepcion")).get("/clientes/nuevo/"));
    }

    @Test
    void registrar_un_cliente_lo_guarda_con_mayuscula_inicial() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/clientes/guardar/",
            "nombres", "cARMEN", "apellidos", "herrera rojas", "identificacion", "88200001", "tipoDocumento", "DNI",
            "ciudad", "oxapampa", "pais", "perú", "telefono", "999111222");

        assertSessionHas(respuesta, "exito", "Cliente registrado correctamente.");
        assertDatabaseHas("cliente", Map.of("identificacion", "88200001", "nombres", "Carmen", "apellidos", "Herrera Rojas", "ciudad", "Oxapampa", "tipo_documento", "DNI"));
    }

    @Test
    void no_se_registra_dos_veces_el_mismo_documento() throws Exception {
        crearCliente("Carmen", "Herrera", "88200002");

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/clientes/guardar/",
            "nombres", "Otra", "apellidos", "Persona", "identificacion", "88200002", "tipoDocumento", "DNI");

        assertRedirect(respuesta, "/clientes/nuevo/");
        assertSessionHas(respuesta, "error", "Ya existe un cliente con esa identificación.");
        assertDatabaseCount("cliente", Map.of("identificacion", "88200002"), 1);
    }

    @Test
    void el_formulario_de_edicion_muestra_los_datos_del_cliente() throws Exception {
        Cliente cliente = crearCliente("Carmen", "Herrera", "88200003");

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).get("/clientes/editar/" + cliente.getId() + "/");

        assertOk(respuesta);
        assertSee(respuesta, "88200003");
    }

    @Test
    void editar_un_cliente_actualiza_sus_datos() throws Exception {
        Cliente cliente = crearCliente("Carmen", "Herrera", "88200004");

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/clientes/guardar/",
            "id", cliente.getId(), "nombres", "Carmen", "apellidos", "Herrera", "identificacion", "88200004", "tipoDocumento", "DNI", "telefono", "988777666");

        assertSessionHas(respuesta, "exito", "Cliente actualizado correctamente.");
        assertDatabaseHas("cliente", Map.of("id", cliente.getId(), "telefono", "988777666"));
    }

    @Test
    void eliminar_un_cliente_sin_estadias_lo_borra() throws Exception {
        Cliente cliente = crearCliente("Carmen", "Herrera", "88200005");

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/clientes/eliminar/" + cliente.getId() + "/");

        assertSessionHas(respuesta, "exito", "Cliente eliminado correctamente.");
        assertDatabaseMissing("cliente", Map.of("id", cliente.getId()));
    }

    @Test
    void no_se_elimina_un_cliente_con_estadias() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");
        Cliente cliente = crearCliente("Carmen", "Herrera", "88200006");
        crearEstadia(cliente, crearHabitacion("Q01", crearCategoria("Categoría QA", 180), 2), diasDesdeHoy(0, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).post("/clientes/eliminar/" + cliente.getId() + "/");

        assertSessionHas(respuesta, "error", "tiene registros asociados");
        assertDatabaseHas("cliente", Map.of("id", cliente.getId()));
    }

    @Test
    void el_alta_rapida_crea_el_cliente_y_devuelve_su_nombre() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/clientes/crear-rapido/",
            "nombres", "rápido", "apellidos", "qa", "identificacion", " 88200007 ", "tipoDocumento", "PASAPORTE", "telefono", "900");

        assertOk(respuesta);
        assertSee(respuesta, "Rápido Qa");
        assertDatabaseHas("cliente", Map.of("identificacion", "88200007", "nombres", "Rápido", "tipo_documento", "PASAPORTE"));
    }

    @Test
    void el_alta_rapida_exige_nombres_apellidos_y_documento() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/clientes/crear-rapido/",
            "nombres", "Rápido", "apellidos", " ", "identificacion", "88200008");

        assertStatus(respuesta, 400);
        assertSee(respuesta, "son obligatorios");
        assertDatabaseMissing("cliente", Map.of("identificacion", "88200008"));
    }

    @Test
    void el_alta_rapida_no_duplica_un_documento() throws Exception {
        crearCliente("Carmen", "Herrera", "88200009");

        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/clientes/crear-rapido/",
            "nombres", "Otra", "apellidos", "Persona", "identificacion", "88200009");

        assertStatus(respuesta, 400);
        assertDatabaseCount("cliente", Map.of("identificacion", "88200009"), 1);
    }

    @Test
    void el_autocompletado_rechaza_un_dni_que_no_tiene_ocho_digitos() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).get("/clientes/dni/1234");

        assertStatus(respuesta, 400);
        assertSee(respuesta, "DNI inválido");
    }
}
