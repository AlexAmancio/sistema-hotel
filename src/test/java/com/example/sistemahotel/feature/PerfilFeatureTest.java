package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Usuario;

// Rutas /perfil/** (HU05 y HU06). Regla RN20.
class PerfilFeatureTest extends FeatureTestCase {

    @Test
    void el_perfil_muestra_los_datos_del_usuario_en_sesion() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).get("/perfil/");

        assertOk(respuesta);
        assertSee(respuesta, "qa_recepcion");
    }

    @Test
    void cambiar_la_contrasena_con_datos_correctos_la_guarda_cifrada() throws Exception {
        Usuario usuario = crearRecepcionista("qa_recepcion");

        MvcResult respuesta = actingAs(usuario).post("/perfil/cambiar-contrasena/", "actual", "Secreto2026", "nueva", "Nueva2026", "confirmar", "Nueva2026");

        assertRedirect(respuesta, "/perfil/");
        assertSessionHas(respuesta, "exito", "Contraseña actualizada correctamente.");
        assertTrue(passwordEncoder.matches("Nueva2026", columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, usuario.getId())));
    }

    @Test
    void con_la_contrasena_actual_incorrecta_no_se_cambia() throws Exception {
        Usuario usuario = crearRecepcionista("qa_recepcion");
        String hash = columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, usuario.getId());

        MvcResult respuesta = actingAs(usuario).post("/perfil/cambiar-contrasena/", "actual", "otra", "nueva", "Nueva2026", "confirmar", "Nueva2026");

        assertSessionHas(respuesta, "error", "La contraseña actual no es correcta.");
        assertDatabaseHas("usuario", Map.of("id", usuario.getId(), "contrasena", hash));
    }

    @Test
    void una_contrasena_nueva_vacia_se_rechaza() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/perfil/cambiar-contrasena/", "actual", "Secreto2026", "nueva", " ", "confirmar", " ");

        assertSessionHas(respuesta, "error", "no puede estar vacía");
    }

    @Test
    void si_la_confirmacion_no_coincide_se_rechaza() throws Exception {
        MvcResult respuesta = actingAs(crearRecepcionista("qa_recepcion")).post("/perfil/cambiar-contrasena/", "actual", "Secreto2026", "nueva", "Nueva2026", "confirmar", "Otra2026");

        assertSessionHas(respuesta, "error", "no coinciden");
    }
}
