package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.PasswordResetToken;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IPasswordResetTokenRepository;

// Verifica de punta a punta la corrección de los defectos D2, D5, D7, D9, D10,
// D11, D12, D13 y D14 (sección XVI del informe), con la aplicación completa
// sobre MySQL. D3, D4 y D6 se verifican en ConfiguracionSinSecretosTest,
// SecurityConfigClaveTest y CustomAuthenticationFailureHandlerTest.
class DefectosCorregidosFeatureTest extends FeatureTestCase {

    @Autowired
    private IPasswordResetTokenRepository tokens;

    private Usuario admin;
    private Usuario recepcion;
    private Categoria categoria;
    private Habitacion habitacion;
    private Cliente cliente;

    @BeforeEach
    void datos() {
        admin = crearAdmin("qa_admin");
        recepcion = crearRecepcionista("qa_recepcion");
        categoria = crearCategoria("Categoría D QA", 180);
        habitacion = crearHabitacion("QD1", categoria, 2);
        cliente = crearCliente("Carmen", "Defecto", "89200001");
    }

    private MvcResult checkIn(String fecha, int noches, Object tarifa, Object pagado, int huespedes) throws Exception {
        return actingAs(recepcion).post("/guardar/", "cliente", cliente.getId(), "habitacion", habitacion.getId(),
            "checkIn", fecha, "noches", noches, "numHuespedes", huespedes, "tarifa", tarifa, "montoPagado", pagado, "metodoPago", "EFECTIVO");
    }

    // ---------------- D2: la tarifa la calcula el servidor ----------------

    @Test
    void d2_la_tarifa_enviada_por_el_navegador_se_ignora_y_se_calcula_en_el_servidor() throws Exception {
        checkIn("2028-03-01T15:00", 2, 1, 0, 1);

        assertDatabaseHas("registro", Map.of("habitacion_id", habitacion.getId(), "tarifa", 360.0));
    }

    @Test
    void d2_en_temporada_alta_el_servidor_cobra_el_precio_alto() throws Exception {
        Habitacion domo = crearHabitacion("QD2", crearCategoriaConTemporada("Domo D QA", 300, 380, "2027-12-20", "2028-01-05"), 2);

        actingAs(recepcion).post("/guardar/", "cliente", cliente.getId(), "habitacion", domo.getId(), "checkIn", "2027-12-26T15:00",
            "noches", 2, "numHuespedes", 1, "tarifa", 10, "montoPagado", 0, "metodoPago", "EFECTIVO");

        assertDatabaseHas("registro", Map.of("habitacion_id", domo.getId(), "tarifa", 760.0));
    }

    @Test
    void d2_al_editar_sin_cambiar_habitacion_fecha_ni_noches_se_conserva_la_tarifa_guardada() throws Exception {
        Registro r = crearEstadia(cliente, habitacion, fecha("2028-03-01T15:00"), 2, 360, 0, EstadoRegistro.ACTIVA, recepcion);
        jdbc.update("UPDATE categoria SET precio = 999 WHERE id = ?", categoria.getId());

        actingAs(recepcion).post("/guardar/", "id", r.getId(), "version", r.getVersion(), "cliente", cliente.getId(), "habitacion", habitacion.getId(),
            "checkIn", "2028-03-01T15:00", "noches", 2, "numHuespedes", 1, "tarifa", 1, "montoPagado", 100, "metodoPago", "EFECTIVO", "estado", "ACTIVA");

        assertDatabaseHas("registro", Map.of("id", r.getId(), "tarifa", 360.0, "monto_pagado", 100.0));
    }

    @Test
    void d2_al_editar_las_noches_la_tarifa_se_recalcula_en_el_servidor() throws Exception {
        Registro r = crearEstadia(cliente, habitacion, fecha("2028-03-01T15:00"), 2, 360, 0, EstadoRegistro.ACTIVA, recepcion);

        actingAs(recepcion).post("/guardar/", "id", r.getId(), "version", r.getVersion(), "cliente", cliente.getId(), "habitacion", habitacion.getId(),
            "checkIn", "2028-03-01T15:00", "noches", 3, "numHuespedes", 1, "tarifa", 1, "montoPagado", 0, "metodoPago", "EFECTIVO", "estado", "ACTIVA");

        assertDatabaseHas("registro", Map.of("id", r.getId(), "noches", 3, "tarifa", 540.0));
    }

    // ---------------- D5: la aplicación no usa root ----------------

    @Test
    void d5_la_aplicacion_se_conecta_con_un_usuario_que_no_es_root() {
        String usuario = columna("SELECT CURRENT_USER()", String.class);

        assertTrue(usuario.startsWith("estadia_app@"), usuario);
    }

    // ---------------- D7: mensaje de bloqueo ----------------

    @Test
    void d7_el_mensaje_de_bloqueo_separa_el_numero_de_la_palabra_minutos() throws Exception {
        crearUsuario("qa_bloqueo", "Secreto2026", false, true);
        String destino = "";
        for (int k = 0; k < 3; k++) {
            destino = URLDecoder.decode(comoInvitado().desdeLaIp("10.73.0.1").post("/login", "username", "qa_bloqueo", "password", "mala")
                .getResponse().getRedirectedUrl(), StandardCharsets.UTF_8);
        }

        assertTrue(destino.contains("Vuelva a intentar en 5 minutos."), destino);
    }

    // ---------------- D9: sesión única ----------------

    private MockHttpSession iniciarSesion(String usuario, String clave) throws Exception {
        MvcResult r = mockMvc.perform(MockMvcRequestBuilders.post("/login").with(csrf())
                .param("username", usuario).param("password", clave)
                .with(req -> { req.setRemoteAddr("10.73.0.9"); return req; }))
            .andReturn();
        assertEquals("/", r.getResponse().getRedirectedUrl());
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    @Test
    void d9_al_iniciar_sesion_en_otro_equipo_la_sesion_anterior_se_cierra() throws Exception {
        crearUsuario("qa_unico", "Secreto2026", false, true);

        MockHttpSession primera = iniciarSesion("qa_unico", "Secreto2026");
        MockHttpSession segunda = iniciarSesion("qa_unico", "Secreto2026");

        MvcResult conLaPrimera = mockMvc.perform(MockMvcRequestBuilders.get("/clientes/").session(primera)).andReturn();
        MvcResult conLaSegunda = mockMvc.perform(MockMvcRequestBuilders.get("/clientes/").session(segunda)).andReturn();

        assertEquals("/login?expirada", conLaPrimera.getResponse().getRedirectedUrl());
        assertEquals(200, conLaSegunda.getResponse().getStatus());
    }

    @Test
    void d9_la_pantalla_de_login_explica_por_que_se_cerro_la_sesion() throws Exception {
        assertSee(comoInvitado().get("/login", "expirada", ""), "se inició sesión con tu usuario en otro equipo");
    }

    // ---------------- D10: bloqueo por IP real ----------------

    @Test
    void d10_cambiar_la_cabecera_x_forwarded_for_no_evade_el_bloqueo() throws Exception {
        crearUsuario("qa_ip", "Secreto2026", false, true);
        for (int k = 0; k < 3; k++) {
            final String falsa = "198.51.100." + k;
            mockMvc.perform(MockMvcRequestBuilders.post("/login").with(csrf()).param("username", "qa_ip").param("password", "mala")
                .header("X-Forwarded-For", falsa).with(req -> { req.setRemoteAddr("10.73.0.2"); return req; }));
        }

        MvcResult conOtraCabecera = mockMvc.perform(MockMvcRequestBuilders.post("/login").with(csrf())
                .param("username", "qa_ip").param("password", "Secreto2026")
                .header("X-Forwarded-For", "198.51.100.99").with(req -> { req.setRemoteAddr("10.73.0.2"); return req; }))
            .andReturn();

        String destino = URLDecoder.decode(conOtraCabecera.getResponse().getRedirectedUrl(), StandardCharsets.UTF_8);
        assertTrue(destino.contains("Vuelva a intentar en"), destino);
        assertDatabaseCount("login_attempts", Map.of("ip_address", "10.73.0.2", "successful", false), 4);
    }

    // ---------------- D11: cabecera del comprobante ----------------

    @Test
    void d11_el_comprobante_muestra_los_datos_del_hotel_y_no_datos_de_ejemplo() throws Exception {
        Registro r = crearEstadia(cliente, habitacion, fecha("2026-01-10T15:00"), 1, 180, 180, EstadoRegistro.FINALIZADA, recepcion);
        actingAs(recepcion).post("/comprobantes/emitir/" + r.getId() + "/");

        MvcResult respuesta = get("/comprobantes/ver/" + r.getId() + "/");

        assertSee(respuesta, "Tunki Lodge");
        assertSee(respuesta, "Oxapampa");
        assertDontSee(respuesta, "20000000001");
        assertDontSee(respuesta, "Av. Ejemplo");
    }

    // ---------------- D12: validaciones en el servidor ----------------

    @Test
    void d12_una_estadia_de_0_noches_no_se_guarda() throws Exception {
        MvcResult respuesta = checkIn("2028-03-01T15:00", 0, 0, 0, 1);

        assertSessionHas(respuesta, "error", "al menos 1 noche");
        assertDatabaseMissing("registro", Map.of("habitacion_id", habitacion.getId()));
    }

    @Test
    void d12_un_pago_negativo_no_se_guarda() throws Exception {
        MvcResult respuesta = checkIn("2028-03-01T15:00", 1, 180, -50, 1);

        assertSessionHas(respuesta, "error", "no puede ser negativo");
        assertDatabaseMissing("registro", Map.of("habitacion_id", habitacion.getId()));
    }

    @Test
    void d12_una_estadia_sin_huespedes_no_se_guarda() throws Exception {
        assertSessionHas(checkIn("2028-03-01T15:00", 1, 180, 0, 0), "error", "al menos 1 huésped");
        assertDatabaseMissing("registro", Map.of("habitacion_id", habitacion.getId()));
    }

    @Test
    void d12_extender_no_permite_reducir_ni_mantener_las_noches() throws Exception {
        Registro r = crearEstadia(cliente, habitacion, fecha("2028-03-01T15:00"), 3, 540, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult menos = actingAs(recepcion).post("/extender/" + r.getId() + "/", "noches", 1);
        MvcResult iguales = post("/extender/" + r.getId() + "/", "noches", 3);

        assertSessionHas(menos, "error", "debe ser mayor que la actual (3)");
        assertSessionHas(iguales, "error", "debe ser mayor que la actual (3)");
        assertDatabaseHas("registro", Map.of("id", r.getId(), "noches", 3, "tarifa", 540.0));
    }

    @Test
    void d12_la_contrasena_de_recuperacion_de_menos_de_5_caracteres_se_rechaza() throws Exception {
        String hash = columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, recepcion.getId());
        PasswordResetToken t = new PasswordResetToken();
        t.setUsuario(recepcion);
        t.setToken("token-d12-qa");
        t.setFechaExpiracion(LocalDateTime.now().plusMinutes(30));
        tokens.save(t);

        MvcResult respuesta = comoInvitado().post("/login/restablecer", "token", "token-d12-qa", "nuevaContrasena", "1234");

        assertEquals("formulario", modelo(respuesta, "estado"));
        assertSee(respuesta, "al menos 5 caracteres");
        assertDatabaseHas("usuario", Map.of("id", recepcion.getId(), "contrasena", hash));
        assertDatabaseHas("password_reset_token", Map.of("token", "token-d12-qa", "usado", false));
    }

    @Test
    void d12_marcar_disponible_solo_cambia_una_habitacion_en_limpieza() throws Exception {
        Habitacion ocupada = crearHabitacion("QD3", categoria, 2, EstadoHabitacion.OCUPADA);
        Habitacion mantenimiento = crearHabitacion("QD4", categoria, 2, EstadoHabitacion.MANTENIMIENTO);

        MvcResult respuesta = actingAs(recepcion).post("/habitaciones/marcar-disponible/" + ocupada.getId() + "/");
        post("/habitaciones/marcar-disponible/" + mantenimiento.getId() + "/");

        assertSessionHas(respuesta, "error", "no está en limpieza");
        assertDatabaseHas("habitacion", Map.of("id", ocupada.getId(), "estado", "OCUPADA"));
        assertDatabaseHas("habitacion", Map.of("id", mantenimiento.getId(), "estado", "MANTENIMIENTO"));
    }

    @Test
    void d12_una_reserva_de_0_noches_no_se_crea() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/reservas/guardar/", "cliente", cliente.getId(), "checkIn", "2028-06-10T15:00",
            "noches", 0, "habitaciones", List.of(habitacion.getId()));

        assertEquals("La reserva debe tener al menos 1 noche.", modelo(respuesta, "error"));
        assertDatabaseMissing("reserva", Map.of("cliente_id", cliente.getId()));
    }

    @Test
    void d12_una_reserva_con_una_habitacion_sin_huespedes_no_se_crea() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/reservas/guardar/", "cliente", cliente.getId(), "checkIn", "2028-06-10T15:00",
            "noches", 1, "habitaciones", List.of(habitacion.getId()), "huespedes_" + habitacion.getId(), 0);

        assertEquals("Cada habitación debe tener al menos 1 huésped.", modelo(respuesta, "error"));
        assertDatabaseMissing("reserva", Map.of("cliente_id", cliente.getId()));
    }

    // ---------------- D13: sin duplicados al editar ----------------

    @Test
    void d13_al_editar_un_cliente_no_se_puede_usar_el_documento_de_otro() throws Exception {
        crearCliente("Otra", "Persona", "89200002");

        MvcResult respuesta = actingAs(recepcion).post("/clientes/guardar/", "id", cliente.getId(), "nombres", "Carmen",
            "apellidos", "Defecto", "identificacion", "89200002", "tipoDocumento", "DNI");

        assertSessionHas(respuesta, "error", "Ya existe otro cliente con esa identificación.");
        assertDatabaseHas("cliente", Map.of("id", cliente.getId(), "identificacion", "89200001"));
    }

    @Test
    void d13_al_editar_un_cliente_puede_conservar_su_propio_documento() throws Exception {
        actingAs(recepcion).post("/clientes/guardar/", "id", cliente.getId(), "nombres", "Carmen",
            "apellidos", "Defecto", "identificacion", "89200001", "tipoDocumento", "DNI", "telefono", "911");

        assertDatabaseHas("cliente", Map.of("id", cliente.getId(), "telefono", "911"));
    }

    @Test
    void d13_al_editar_un_usuario_no_se_puede_usar_el_username_de_otro() throws Exception {
        MvcResult respuesta = actingAs(admin).post("/usuarios/guardar/", "id", recepcion.getId(), "username", "qa_admin",
            "nombres", "Usuario", "apellidos", "Prueba", "contrasena", "", "activo", "true");

        assertSessionHas(respuesta, "error", "username repetido");
        assertDatabaseHas("usuario", Map.of("id", recepcion.getId(), "username", "qa_recepcion"));
        assertDatabaseCount("usuario", Map.of("username", "qa_admin"), 1);
    }

    @Test
    void d13_la_base_de_datos_tambien_rechaza_documentos_y_usernames_repetidos() {
        assertThrows(DataIntegrityViolationException.class, () ->
            jdbc.update("INSERT INTO cliente (nombres, apellidos, identificacion) VALUES ('X', 'Y', '89200001')"));
        assertThrows(DataIntegrityViolationException.class, () ->
            jdbc.update("INSERT INTO usuario (username, nombres, apellidos, contrasena, es_admin, activo) VALUES ('qa_admin', 'X', 'Y', 'z', 0, 1)"));
    }

    // ---------------- D14: salida a las 12:00 también en el cruce ----------------

    @Test
    void d14_otra_estadia_no_puede_empezar_antes_de_las_12_del_dia_de_salida() throws Exception {
        crearEstadia(crearCliente("Primer", "Huésped", "89200003"), habitacion, fecha("2028-03-01T10:28"), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult a_las_11 = checkIn("2028-03-02T11:00", 1, 180, 0, 1);

        assertSessionHas(a_las_11, "error", "se cruza con esas fechas");
        assertDatabaseMissing("registro", Map.of("cliente_id", cliente.getId()));
    }

    @Test
    void d14_otra_estadia_si_puede_empezar_desde_las_12_del_dia_de_salida() throws Exception {
        crearEstadia(crearCliente("Primer", "Huésped", "89200004"), habitacion, fecha("2028-03-01T10:28"), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        checkIn("2028-03-02T12:00", 1, 180, 0, 1);

        assertDatabaseHas("registro", Map.of("cliente_id", cliente.getId(), "estado", "ACTIVA"));
    }

    @Test
    void d14_la_disponibilidad_no_ofrece_la_habitacion_antes_de_las_12_del_dia_de_salida() throws Exception {
        crearEstadia(crearCliente("Primer", "Huésped", "89200005"), habitacion, fecha("2028-03-01T10:28"), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        assertDontSee(actingAs(recepcion).get("/habitaciones/disponibles", "checkIn", "2028-03-02T11:00", "noches", 1), "\"numero\":\"QD1\"");
        assertSee(get("/habitaciones/disponibles", "checkIn", "2028-03-02T12:00", "noches", 1), "\"numero\":\"QD1\"");
    }
}
