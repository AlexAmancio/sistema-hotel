package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Usuario;

// Rutas /usuarios/** (HU09 a HU13). Reglas RN12, RN14, RN21, RN27 y RN35.
class UsuariosFeatureTest extends FeatureTestCase {

    @Test
    void el_administrador_ve_la_lista_de_usuarios() throws Exception {
        Usuario admin = crearAdmin("qa_admin");
        crearRecepcionista("qa_recepcion");

        MvcResult respuesta = actingAs(admin).get("/usuarios/");

        assertOk(respuesta);
        assertSee(respuesta, "qa_recepcion");
    }

    @Test
    void recepcion_tambien_puede_ver_la_lista_de_usuarios() throws Exception {
        assertOk(actingAs(crearRecepcionista("qa_recepcion")).get("/usuarios/"));
    }

    @Test
    void el_formulario_de_nuevo_usuario_se_muestra_al_administrador() throws Exception {
        assertOk(actingAs(crearAdmin("qa_admin")).get("/usuarios/nuevo/"));
    }

    @Test
    void recepcion_no_puede_abrir_el_formulario_de_nuevo_usuario() throws Exception {
        assertForbidden(actingAs(crearRecepcionista("qa_recepcion")).get("/usuarios/nuevo/"));
    }

    @Test
    void crear_usuario_guarda_la_contrasena_cifrada_y_el_nombre_con_mayuscula() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/usuarios/guardar/",
            "username", "qa_nuevo", "nombres", "maría", "apellidos", "quispe", "correo", "qa_nuevo@tunki.test",
            "contrasena", "Clave2026", "activo", "true");

        assertSessionHas(respuesta, "exito", "Usuario creado correctamente.");
        assertDatabaseHas("usuario", Map.of("username", "qa_nuevo", "nombres", "María", "apellidos", "Quispe"));
        String hash = columna("SELECT contrasena FROM usuario WHERE username = ?", String.class, "qa_nuevo");
        assertTrue(passwordEncoder.matches("Clave2026", hash));
    }

    @Test
    void crear_usuario_sin_contrasena_no_lo_guarda() throws Exception {
        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/usuarios/guardar/",
            "username", "qa_nuevo", "nombres", "María", "apellidos", "Quispe", "contrasena", "");

        assertRedirect(respuesta, "/usuarios/nuevo/");
        assertSessionHas(respuesta, "error", "La contraseña es obligatoria");
        assertDatabaseMissing("usuario", Map.of("username", "qa_nuevo"));
    }

    @Test
    void crear_usuario_con_username_repetido_no_lo_duplica() throws Exception {
        crearRecepcionista("qa_repetido");

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/usuarios/guardar/",
            "username", "qa_repetido", "nombres", "Otra", "apellidos", "Persona", "contrasena", "Clave2026");

        assertSessionHas(respuesta, "error", "username repetido");
        assertDatabaseCount("usuario", Map.of("username", "qa_repetido"), 1);
    }

    @Test
    void editar_usuario_sin_escribir_contrasena_conserva_la_anterior() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");
        String hashAnterior = columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, recepcion.getId());

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/usuarios/guardar/",
            "id", recepcion.getId(), "username", "qa_recepcion", "nombres", "Lucía", "apellidos", "Rojas", "contrasena", "", "activo", "true");

        assertSessionHas(respuesta, "exito", "Usuario actualizado correctamente.");
        assertDatabaseHas("usuario", Map.of("id", recepcion.getId(), "nombres", "Lucía", "contrasena", hashAnterior));
    }

    @Test
    void el_formulario_de_edicion_muestra_los_datos_del_usuario() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).get("/usuarios/editar/" + recepcion.getId() + "/");

        assertOk(respuesta);
        assertSee(respuesta, "qa_recepcion");
    }

    @Test
    void desactivar_y_activar_un_usuario_cambia_su_estado() throws Exception {
        Usuario admin = crearAdmin("qa_admin");
        Usuario recepcion = crearRecepcionista("qa_recepcion");

        assertSessionHas(actingAs(admin).post("/usuarios/desactivar/" + recepcion.getId() + "/"), "exito", "desactivado");
        assertDatabaseHas("usuario", Map.of("id", recepcion.getId(), "activo", false));

        actingAs(admin).post("/usuarios/activar/" + recepcion.getId() + "/");
        assertDatabaseHas("usuario", Map.of("id", recepcion.getId(), "activo", true));
    }

    @Test
    void eliminar_un_usuario_sin_registros_lo_borra() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/usuarios/eliminar/" + recepcion.getId() + "/");

        assertSessionHas(respuesta, "exito", "Usuario eliminado correctamente.");
        assertDatabaseMissing("usuario", Map.of("id", recepcion.getId()));
    }

    @Test
    void no_se_elimina_un_usuario_que_registro_estadias() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");
        Habitacion habitacion = crearHabitacion("Q01", crearCategoria("Categoría QA", 180), 2);
        Cliente cliente = crearCliente("Carmen", "Herrera", "88100001");
        crearEstadia(cliente, habitacion, diasDesdeHoy(0, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(crearAdmin("qa_admin")).post("/usuarios/eliminar/" + recepcion.getId() + "/");

        assertSessionHas(respuesta, "error", "tiene registros asociados");
        assertDatabaseHas("usuario", Map.of("id", recepcion.getId()));
    }

    @Test
    void no_se_puede_eliminar_ni_desactivar_al_unico_administrador() throws Exception {
        Usuario admin = crearAdmin("qa_admin");
        // En esta prueba qa_admin queda como único administrador (se deshace al terminar).
        jdbc.update("UPDATE usuario SET es_admin = 0 WHERE id <> ?", admin.getId());

        assertSessionHas(actingAs(admin).post("/usuarios/eliminar/" + admin.getId() + "/"), "error", "único administrador");
        assertSessionHas(actingAs(admin).post("/usuarios/desactivar/" + admin.getId() + "/"), "error", "único administrador");
        assertDatabaseHas("usuario", Map.of("id", admin.getId(), "activo", true));
    }

    @Test
    void recepcion_no_puede_eliminar_usuarios() throws Exception {
        Usuario otro = crearRecepcionista("qa_otro");

        assertForbidden(actingAs(crearRecepcionista("qa_recepcion")).post("/usuarios/eliminar/" + otro.getId() + "/"));
        assertDatabaseHas("usuario", Map.of("id", otro.getId()));
    }
}
