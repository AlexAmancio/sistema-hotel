package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Usuario;

// Inicio y cierre de sesión (HU01, HU02 y HU07). Reglas RN11, RN21 y RN35.
class AutenticacionFeatureTest extends FeatureTestCase {

    private String destino(MvcResult respuesta) {
        return URLDecoder.decode(respuesta.getResponse().getRedirectedUrl(), StandardCharsets.UTF_8);
    }

    @Test
    void la_pantalla_de_inicio_de_sesion_es_publica() throws Exception {
        MvcResult respuesta = comoInvitado().get("/login");

        assertOk(respuesta);
        assertSee(respuesta, "Recuérdame");
    }

    @Test
    void login_con_credenciales_correctas_entra_y_registra_el_intento_exitoso() throws Exception {
        crearUsuario("qa_recepcion", "Secreto2026", false, true);

        MvcResult respuesta = comoInvitado().desdeLaIp("10.70.0.1").post("/login", "username", "qa_recepcion", "password", "Secreto2026");

        assertRedirect(respuesta, "/");
        assertDatabaseHas("login_attempts", Map.of("username", "qa_recepcion", "ip_address", "10.70.0.1", "successful", true));
    }

    @Test
    void login_con_contrasena_incorrecta_vuelve_al_login_y_registra_el_fallo() throws Exception {
        crearUsuario("qa_recepcion", "Secreto2026", false, true);

        MvcResult respuesta = comoInvitado().desdeLaIp("10.70.0.2").post("/login", "username", "qa_recepcion", "password", "incorrecta");

        assertTrue(destino(respuesta).startsWith("/login?error"), destino(respuesta));
        assertTrue(destino(respuesta).contains("Verifique sus credenciales"));
        assertDatabaseHas("login_attempts", Map.of("username", "qa_recepcion", "ip_address", "10.70.0.2", "successful", false));
    }

    @Test
    void login_con_usuario_inexistente_falla() throws Exception {
        MvcResult respuesta = comoInvitado().desdeLaIp("10.70.0.3").post("/login", "username", "qa_no_existe", "password", "cualquiera");

        assertTrue(destino(respuesta).startsWith("/login?error"));
    }

    @Test
    void un_usuario_desactivado_no_puede_entrar() throws Exception {
        crearUsuario("qa_inactivo", "Secreto2026", false, false);

        MvcResult respuesta = comoInvitado().desdeLaIp("10.70.0.4").post("/login", "username", "qa_inactivo", "password", "Secreto2026");

        assertTrue(destino(respuesta).startsWith("/login?error"));
    }

    @Test
    void tres_intentos_fallidos_bloquean_la_ip_aunque_despues_la_contrasena_sea_correcta() throws Exception {
        crearUsuario("qa_recepcion", "Secreto2026", false, true);
        for (int k = 0; k < 3; k++) {
            comoInvitado().desdeLaIp("10.70.0.5").post("/login", "username", "qa_recepcion", "password", "mala" + k);
        }

        MvcResult respuesta = comoInvitado().desdeLaIp("10.70.0.5").post("/login", "username", "qa_recepcion", "password", "Secreto2026");

        assertTrue(destino(respuesta).contains("Vuelva a intentar en"), destino(respuesta));
        assertDatabaseCount("login_attempts", Map.of("ip_address", "10.70.0.5", "successful", false), 4);
    }

    @Test
    void el_bloqueo_es_por_ip_y_no_afecta_a_otra_ip() throws Exception {
        crearUsuario("qa_recepcion", "Secreto2026", false, true);
        for (int k = 0; k < 3; k++) {
            comoInvitado().desdeLaIp("10.70.0.6").post("/login", "username", "qa_recepcion", "password", "mala" + k);
        }

        assertRedirect(comoInvitado().desdeLaIp("10.70.0.7").post("/login", "username", "qa_recepcion", "password", "Secreto2026"), "/");
    }

    @Test
    void sin_sesion_una_pagina_interna_redirige_al_login() throws Exception {
        MvcResult respuesta = comoInvitado().get("/clientes/");

        assertTrue(respuesta.getResponse().getRedirectedUrl().endsWith("/login"));
    }

    @Test
    void cerrar_sesion_guarda_la_hora_de_cierre_y_vuelve_al_login() throws Exception {
        Usuario recepcion = crearRecepcionista("qa_recepcion");

        MvcResult respuesta = actingAs(recepcion).post("/logout");

        assertRedirect(respuesta, "/login?logout");
        assertTrue(columna("SELECT COUNT(*) FROM usuario WHERE id = ? AND logout_time IS NOT NULL", Integer.class, recepcion.getId()) == 1);
    }
}
