package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Usuario;

// Instalación inicial con la base de demostración versionada en
// bd/respaldo_bd_actual.sql (como InstalacionInicialTest en Laravel): la cuenta
// de administrador del README entra, los datos de ejemplo están completos y
// cada pantalla carga con datos reales según el rol.
class InstalacionInicialFeatureTest extends FeatureTestCase {

    private Usuario demo(String username) {
        return usuarios.findByUsername(username).orElseThrow();
    }

    @Test
    void el_administrador_documentado_en_el_readme_puede_iniciar_sesion() throws Exception {
        MvcResult respuesta = comoInvitado().desdeLaIp("10.72.0.1").post("/login", "username", "elon", "password", "admin123");

        assertRedirect(respuesta, "/");
        assertTrue(demo("elon").isEsAdmin());
    }

    @Test
    void la_base_de_demostracion_trae_los_datos_de_ejemplo() {
        assertEquals(6, contar("categoria", Map.of()));
        assertEquals(45, contar("habitacion", Map.of()));
        assertEquals(9, contar("usuario", Map.of()));
        assertEquals(162, contar("cliente", Map.of()));
        assertEquals(60, contar("registro", Map.of()));
        assertEquals(5, contar("producto_consumo", Map.of()));
        assertEquals(4, contar("comprobante", Map.of()));
    }

    @Test
    void hay_un_solo_administrador_y_todas_las_cuentas_estan_activas() {
        assertEquals(1, contar("usuario", Map.of("es_admin", true)));
        assertEquals(9, contar("usuario", Map.of("activo", true)));
    }

    @Test
    void ninguna_contrasena_de_demostracion_esta_en_texto_plano() {
        assertEquals(0, columna("SELECT COUNT(*) FROM usuario WHERE contrasena NOT LIKE '$2a$%'", Integer.class));
    }

    @Test
    void los_datos_de_ejemplo_son_consistentes() {
        assertEquals(0, columna("SELECT COUNT(*) FROM categoria WHERE precio <= 0", Integer.class));
        assertEquals(0, columna("SELECT COUNT(*) FROM habitacion WHERE categoria_id IS NULL OR aforo < 1", Integer.class));
        assertEquals(0, columna("SELECT COUNT(*) FROM (SELECT numero FROM habitacion GROUP BY numero HAVING COUNT(*) > 1) d", Integer.class));
        assertEquals(0, columna("SELECT COUNT(*) FROM (SELECT identificacion FROM cliente GROUP BY identificacion HAVING COUNT(*) > 1) d", Integer.class));
        assertEquals(0, columna("SELECT COUNT(*) FROM registro WHERE cliente_id IS NULL OR habitacion_id IS NULL OR noches < 1", Integer.class));
        assertEquals(0, columna("SELECT COUNT(*) FROM (SELECT serie, correlativo FROM comprobante GROUP BY serie, correlativo HAVING COUNT(*) > 1) d", Integer.class));
        assertEquals(0, columna("SELECT COUNT(*) FROM comprobante c JOIN registro r ON r.id = c.registro_id WHERE r.estado <> 'FINALIZADA'", Integer.class));
    }

    @Test
    void el_administrador_abre_todas_las_pantallas_con_los_datos_reales() throws Exception {
        actingAs(demo("elon"));
        for (String ruta : List.of("/", "/hoy/", "/clientes/", "/habitaciones/", "/categorias/", "/reservas/nueva/", "/nuevo/",
                "/productos-consumo/", "/productos-consumo/nuevo/", "/usuarios/", "/usuarios/nuevo/", "/perfil/",
                "/reportes/", "/dashboard/", "/ocupacion/")) {
            assertEquals(200, get(ruta).getResponse().getStatus(), "Falló " + ruta);
        }
        assertOk(get("/buscar", "q", "a"));
    }

    @Test
    void el_detalle_de_cada_estadia_de_demostracion_carga() throws Exception {
        actingAs(demo("elon"));
        List<Integer> ids = jdbc.queryForList("SELECT id FROM registro", Integer.class);
        for (int id : ids) {
            assertEquals(200, get("/editar/" + id + "/").getResponse().getStatus(), "Falló /editar/" + id);
            assertEquals(200, get("/consumos/" + id + "/").getResponse().getStatus(), "Falló /consumos/" + id);
        }
        for (int id : jdbc.queryForList("SELECT registro_id FROM comprobante", Integer.class)) {
            assertEquals(200, get("/comprobantes/ver/" + id + "/").getResponse().getStatus(), "Falló /comprobantes/ver/" + id);
        }
    }

    @Test
    void una_recepcionista_de_demostracion_trabaja_pero_no_administra() throws Exception {
        actingAs(demo("mquispe"));

        for (String ruta : List.of("/", "/clientes/", "/habitaciones/", "/reservas/nueva/", "/productos-consumo/", "/usuarios/", "/reportes/")) {
            assertEquals(200, get(ruta).getResponse().getStatus(), "Falló " + ruta);
        }
        assertForbidden(get("/usuarios/nuevo/"));
        assertForbidden(get("/productos-consumo/nuevo/"));
        assertForbidden(post("/usuarios/eliminar/" + demo("elon").getId() + "/"));
        assertDatabaseHas("usuario", Map.of("username", "elon"));
    }
}
