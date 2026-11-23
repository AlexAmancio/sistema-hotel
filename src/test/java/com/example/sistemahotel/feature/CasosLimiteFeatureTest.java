package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.controllers.DashboardController.FilaReporte;
import com.example.sistemahotel.controllers.OcupacionController.FilaOcupacion;
import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Casos límite recorriendo la aplicación completa: edición simultánea de una
// habitación, cliente con dos estadías activas, cambio de habitación cuando la
// anterior la sigue usando otra estadía, reportes mensuales, calendario con
// estadías que pasan los bordes y formularios con datos ausentes.
class CasosLimiteFeatureTest extends FeatureTestCase {

    private Usuario admin;
    private Usuario recepcion;
    private Categoria categoria;
    private Cliente cliente;

    @BeforeEach
    void datos() {
        admin = crearAdmin("qa_admin");
        recepcion = crearRecepcionista("qa_recepcion");
        categoria = crearCategoria("Categoría Límite QA", 150);
        cliente = crearCliente("Lucía", "Límite", "89100001");
    }

    @Test
    void editar_una_habitacion_que_otra_persona_ya_modifico_avisa_y_no_la_pisa() throws Exception {
        Habitacion h = crearHabitacion("QL1", categoria, 2);
        refrescar();
        jdbc.update("UPDATE habitacion SET version = version + 1, aforo = 5 WHERE id = ?", h.getId());

        MvcResult respuesta = actingAs(admin).post("/habitaciones/guardar/", "id", h.getId(), "version", 0,
            "numero", "QL1", "piso", "9", "aforo", "3", "categoria", categoria.getId(), "estado", "DISPONIBLE");

        assertSessionHas(respuesta, "error", "fue modificada por otra persona");
        assertDatabaseHas("habitacion", Map.of("id", h.getId(), "aforo", 5));
    }

    @Test
    void la_tarifa_con_una_fecha_que_no_se_entiende_usa_el_precio_base() throws Exception {
        Categoria temporada = crearCategoriaConTemporada("Temporada Límite QA", 300, 380, "2020-01-01", "2099-12-31");
        Habitacion h = crearHabitacion("QL2", temporada, 2);

        assertSee(actingAs(recepcion).get("/habitaciones/calcular-tarifa/" + h.getId(), "noches", 2, "checkIn", "mañana"), "600.0");
        assertSee(get("/habitaciones/calcular-tarifa/" + h.getId(), "noches", 2), "600.0");
    }

    @Test
    void el_formulario_de_check_in_avisa_si_el_cliente_ya_tiene_dos_estadias_activas() throws Exception {
        crearEstadia(cliente, crearHabitacion("QL3", categoria, 2), diasDesdeHoy(0, 12), 1, 150, 0, EstadoRegistro.ACTIVA, recepcion);
        crearEstadia(cliente, crearHabitacion("QL4", categoria, 2), diasDesdeHoy(0, 12), 1, 150, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).get("/nuevo/");

        assertOk(respuesta);
        String habitacion = ((Map<?, ?>) modelo(respuesta, "clientesConReservaActiva")).get(cliente.getId()).toString();
        assertTrue(habitacion.equals("QL3") || habitacion.equals("QL4"));
    }

    @Test
    void cambiar_de_habitacion_no_libera_la_anterior_si_otra_estadia_activa_la_sigue_usando() throws Exception {
        Habitacion compartida = crearHabitacion("QL5", categoria, 4);
        Habitacion nueva = crearHabitacion("QL6", categoria, 4);
        Registro mia = crearEstadia(cliente, compartida, fecha("2028-11-01T12:00"), 2, 300, 0, EstadoRegistro.ACTIVA, recepcion);
        crearEstadia(crearCliente("Otra", "Familia", "89100002"), compartida, fecha("2028-11-10T12:00"), 2, 300, 0, EstadoRegistro.ACTIVA, recepcion);

        actingAs(recepcion).post("/guardar/", "id", mia.getId(), "version", mia.getVersion(), "cliente", cliente.getId(),
            "habitacion", nueva.getId(), "checkIn", "2028-11-01T12:00", "noches", 2, "numHuespedes", 1, "tarifa", 300,
            "montoPagado", 0, "metodoPago", "EFECTIVO", "estado", "ACTIVA");

        assertDatabaseHas("registro", Map.of("id", mia.getId(), "habitacion_id", nueva.getId()));
        assertDatabaseHas("habitacion", Map.of("id", compartida.getId(), "estado", "OCUPADA"));
        assertDatabaseHas("habitacion", Map.of("id", nueva.getId(), "estado", "OCUPADA"));
    }

    @Test
    void editar_solo_el_pago_no_revalida_el_aforo_aunque_la_habitacion_haya_bajado_de_aforo() throws Exception {
        Habitacion h = crearHabitacion("QL7", categoria, 4);
        Registro r = crearEstadia(cliente, h, fecha("2028-11-01T12:00"), 1, 150, 0, EstadoRegistro.ACTIVA, recepcion);
        jdbc.update("UPDATE registro SET num_huespedes = 4 WHERE id = ?", r.getId());
        jdbc.update("UPDATE habitacion SET aforo = 2 WHERE id = ?", h.getId());

        actingAs(recepcion).post("/guardar/", "id", r.getId(), "version", r.getVersion(), "cliente", cliente.getId(),
            "habitacion", h.getId(), "checkIn", "2028-11-01T12:00", "noches", 1, "numHuespedes", 4, "tarifa", 150,
            "montoPagado", 150, "metodoPago", "EFECTIVO", "estado", "ACTIVA");

        assertDatabaseHas("registro", Map.of("id", r.getId(), "monto_pagado", 150.0));
    }

    @Test
    void la_fila_del_mes_actual_suma_lo_cobrado_y_no_las_canceladas() throws Exception {
        List<?> antes = (List<?>) modelo(actingAs(recepcion).get("/reportes/"), "filas");
        double ingresosAntes = ((FilaReporte) antes.get(antes.size() - 1)).ingresos;

        crearEstadia(cliente, crearHabitacion("QL8", categoria, 2), diasDesdeHoy(0, 0), 1, 150, 150, EstadoRegistro.ACTIVA, recepcion);
        crearEstadia(cliente, crearHabitacion("QL9", categoria, 2), diasDesdeHoy(0, 0), 1, 150, 999, EstadoRegistro.CANCELADA, recepcion);

        List<?> despues = (List<?>) modelo(get("/reportes/"), "filas");
        FilaReporte mesActual = (FilaReporte) despues.get(despues.size() - 1);
        assertEquals(ingresosAntes + 150, mesActual.ingresos, 0.001);
        assertTrue(mesActual.ocupacionPromedio > 0);
    }

    @Test
    void una_habitacion_en_mantenimiento_cuenta_aparte_en_reportes() throws Exception {
        long antes = (long) modelo(actingAs(recepcion).get("/reportes/"), "enMantenimiento");

        crearHabitacion("QM1", categoria, 2, EstadoHabitacion.MANTENIMIENTO);

        assertEquals(antes + 1, (long) modelo(get("/reportes/"), "enMantenimiento"));
        assertOk(get("/habitaciones/"));
    }

    @Test
    void una_estadia_que_empieza_antes_y_termina_despues_del_calendario_ocupa_todo_el_rango() throws Exception {
        Habitacion h = crearHabitacion("QL10", categoria, 2);
        crearEstadia(cliente, h, fecha("2028-03-20T12:00"), 40, 6000, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).get("/ocupacion/", "desde", "2028-04-01");

        FilaOcupacion fila = ((List<?>) modelo(respuesta, "filas")).stream().map(f -> (FilaOcupacion) f)
            .filter(f -> f.habitacion.getNumero().equals("QL10")).findFirst().orElseThrow();
        assertEquals(0, fila.bloques.get(0).colInicio);
        assertEquals(14, fila.bloques.get(0).span);
    }

    @Test
    void el_historial_del_cliente_muestra_su_estadia_mas_reciente_primero() throws Exception {
        crearEstadia(cliente, crearHabitacion("QL11", categoria, 2), fecha("2026-01-10T12:00"), 1, 150, 150, EstadoRegistro.FINALIZADA, recepcion);
        crearEstadia(cliente, crearHabitacion("QL12", categoria, 2), fecha("2026-06-10T12:00"), 1, 150, 150, EstadoRegistro.FINALIZADA, recepcion);

        List<?> historial = (List<?>) modelo(actingAs(recepcion).get("/clientes/editar/" + cliente.getId() + "/"), "historial");

        assertEquals(2, historial.size());
        assertEquals("QL12", ((Registro) historial.get(0)).getHabitacion().getNumero());
    }

    @Test
    void el_csv_deja_vacio_registrado_por_si_la_estadia_no_tiene_usuario() throws Exception {
        crearEstadia(cliente, crearHabitacion("QL13", categoria, 2), fecha("2028-05-10T12:00"), 1, 150, 0, EstadoRegistro.ACTIVA, null);

        String csv = actingAs(recepcion).get("/exportar/", "q", "89100001").getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(csv.trim().endsWith(",\"\""), csv);
    }

    @Test
    void cerrar_sesion_sin_haber_entrado_vuelve_al_login_sin_error() throws Exception {
        assertRedirect(comoInvitado().post("/logout"), "/login?logout");
    }

    @Test
    void el_alta_rapida_rechaza_nombres_o_documento_en_blanco() throws Exception {
        actingAs(recepcion);
        assertStatus(post("/clientes/crear-rapido/", "nombres", " ", "apellidos", "Límite", "identificacion", "89100003"), 400);
        assertStatus(post("/clientes/crear-rapido/", "nombres", "Ana", "apellidos", "Límite", "identificacion", " "), 400);
        assertDatabaseMissing("cliente", Map.of("identificacion", "89100003"));
    }

    @Test
    void el_alta_rapida_con_un_tipo_de_documento_desconocido_lo_guarda_como_dni() throws Exception {
        actingAs(recepcion).post("/clientes/crear-rapido/", "nombres", "Ana", "apellidos", "Límite", "identificacion", "89100004", "tipoDocumento", "LICENCIA");

        assertDatabaseHas("cliente", Map.of("identificacion", "89100004", "tipo_documento", "DNI"));
    }

    @Test
    void una_categoria_con_solo_el_inicio_de_temporada_se_guarda_y_cobra_el_precio_base() throws Exception {
        actingAs(admin).post("/categorias/guardar/", "nombre", "Media Temporada QA", "precio", "200",
            "precioTemporadaAlta", "260", "temporadaAltaInicio", "2020-01-01");
        int id = columna("SELECT id FROM categoria WHERE nombre = ?", Integer.class, "Media Temporada QA");
        Habitacion h = crearHabitacion("QL14", categorias.findById(id).orElseThrow(), 2);

        assertSee(get("/habitaciones/calcular-tarifa/" + h.getId(), "noches", 1, "checkIn", "2028-01-01T12:00"), "200.0");
    }

    @Test
    void un_producto_sin_el_campo_nombre_no_se_guarda() throws Exception {
        MvcResult respuesta = actingAs(admin).post("/productos-consumo/guardar/", "precioSugerido", "10");

        assertSessionHas(respuesta, "error", "El nombre es obligatorio.");
    }

    @Test
    void la_contrasena_nueva_en_blanco_no_reemplaza_la_actual() throws Exception {
        String hash = columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, recepcion.getId());

        MvcResult respuesta = actingAs(recepcion).post("/perfil/cambiar-contrasena/", "actual", "Secreto2026", "nueva", "", "confirmar", "");

        assertSessionHas(respuesta, "error", "no puede estar vacía");
        assertDatabaseHas("usuario", Map.of("id", recepcion.getId(), "contrasena", hash));
    }

    @Test
    void con_dos_administradores_activos_se_puede_eliminar_uno() throws Exception {
        Usuario segundo = crearAdmin("qa_admin2");

        assertSessionHas(actingAs(admin).post("/usuarios/eliminar/" + segundo.getId() + "/"), "exito", "Usuario eliminado correctamente.");
        assertDatabaseMissing("usuario", Map.of("id", segundo.getId()));
    }

    @Test
    void cambiar_la_contrasena_de_otro_usuario_desde_su_edicion_la_cifra() throws Exception {
        actingAs(admin).post("/usuarios/guardar/", "id", recepcion.getId(), "username", "qa_recepcion", "nombres", "Usuario",
            "apellidos", "Prueba", "contrasena", "Cambiada2026", "activo", "true");

        String hash = columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, recepcion.getId());
        assertTrue(passwordEncoder.matches("Cambiada2026", hash));
    }
}
