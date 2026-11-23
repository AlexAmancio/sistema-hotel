package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.controllers.OcupacionController.FilaOcupacion;
import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Usuario;

// Búsqueda global, Reportes (dashboard) y calendario de Ocupación
// (HU60 a HU66). Reglas RN13, RN37 a RN39 y RN43.
class ConsultasYReportesFeatureTest extends FeatureTestCase {

    private Usuario recepcion;
    private Categoria categoria;
    private Cliente cliente;

    @BeforeEach
    void datos() {
        recepcion = crearRecepcionista("qa_recepcion");
        categoria = crearCategoria("Categoría QA", 180);
        cliente = crearCliente("Zoraida", "Quintana", "88800001");
    }

    @Test
    void la_busqueda_encuentra_clientes_habitaciones_y_estadias() throws Exception {
        Habitacion habitacion = crearHabitacion("QZ9", categoria, 2);
        crearEstadia(cliente, habitacion, diasDesdeHoy(0, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult porCliente = actingAs(recepcion).get("/buscar", "q", "Zoraida");
        MvcResult porHabitacion = get("/buscar", "q", "QZ9");

        assertOk(porCliente);
        assertEquals(1, ((List<?>) modelo(porCliente, "clientes")).size());
        assertEquals(1, ((List<?>) modelo(porHabitacion, "habitaciones")).size());
        assertEquals(1, ((List<?>) modelo(porHabitacion, "registros")).size());
    }

    @Test
    void una_busqueda_vacia_no_consulta_nada() throws Exception {
        MvcResult respuesta = actingAs(recepcion).get("/buscar", "q", " ");

        assertOk(respuesta);
        assertTrue(modelo(respuesta, "clientes") == null);
    }

    @Test
    void reportes_cuenta_ocupadas_ingresos_del_dia_y_saldo_pendiente() throws Exception {
        long ocupadasAntes = (long) modelo(actingAs(recepcion).get("/reportes/"), "ocupadas");
        double ingresosAntes = (double) modelo(get("/reportes/"), "ingresosHoy");
        double saldoAntes = (double) modelo(get("/reportes/"), "saldoPendienteTotal");

        crearEstadia(cliente, crearHabitacion("Q60", categoria, 2), diasDesdeHoy(0, 0), 2, 360, 100, EstadoRegistro.ACTIVA, recepcion);
        crearEstadia(crearCliente("Otro", "Huésped", "88800002"), crearHabitacion("Q61", categoria, 2), diasDesdeHoy(0, 0), 1, 180, 180, EstadoRegistro.CANCELADA, recepcion);

        MvcResult respuesta = get("/reportes/");

        assertOk(respuesta);
        assertEquals(ocupadasAntes + 1, (long) modelo(respuesta, "ocupadas"));
        assertEquals(ingresosAntes + 100, (double) modelo(respuesta, "ingresosHoy"), 0.001);
        assertEquals(saldoAntes + 260, (double) modelo(respuesta, "saldoPendienteTotal"), 0.001);
    }

    @Test
    void reportes_cuenta_las_estadias_vencidas() throws Exception {
        long antes = (long) modelo(actingAs(recepcion).get("/reportes/"), "vencidas");

        crearEstadia(cliente, crearHabitacion("Q62", categoria, 2), diasDesdeHoy(-4, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        assertEquals(antes + 1, (long) modelo(get("/reportes/"), "vencidas"));
    }

    @Test
    void el_dashboard_tambien_responde_en_su_ruta() throws Exception {
        assertOk(actingAs(recepcion).get("/dashboard/"));
    }

    @Test
    void el_calendario_de_ocupacion_muestra_el_bloque_de_la_estadia() throws Exception {
        Habitacion habitacion = crearHabitacion("Q63", categoria, 2);
        crearEstadia(cliente, habitacion, fecha("2028-04-03T12:00"), 3, 540, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).get("/ocupacion/", "desde", "2028-04-01");

        assertOk(respuesta);
        FilaOcupacion fila = ((List<?>) modelo(respuesta, "filas")).stream()
            .map(f -> (FilaOcupacion) f)
            .filter(f -> f.habitacion.getNumero().equals("Q63"))
            .findFirst().orElseThrow();
        assertEquals(1, fila.bloques.size());
        assertEquals(2, fila.bloques.get(0).colInicio);
        assertEquals(3, fila.bloques.get(0).span);
    }

    @Test
    void el_calendario_no_muestra_las_estadias_canceladas() throws Exception {
        Habitacion habitacion = crearHabitacion("Q64", categoria, 2);
        crearEstadia(cliente, habitacion, fecha("2028-04-03T12:00"), 2, 360, 0, EstadoRegistro.CANCELADA, recepcion);

        MvcResult respuesta = actingAs(recepcion).get("/ocupacion/", "desde", "2028-04-01");

        FilaOcupacion fila = ((List<?>) modelo(respuesta, "filas")).stream()
            .map(f -> (FilaOcupacion) f)
            .filter(f -> f.habitacion.getNumero().equals("Q64"))
            .findFirst().orElseThrow();
        assertTrue(fila.bloques.isEmpty());
    }

    @Test
    void sin_sesion_ni_reportes_ni_ocupacion_son_visibles() throws Exception {
        assertTrue(comoInvitado().get("/reportes/").getResponse().getRedirectedUrl().endsWith("/login"));
        assertTrue(get("/ocupacion/").getResponse().getRedirectedUrl().endsWith("/login"));
    }
}
