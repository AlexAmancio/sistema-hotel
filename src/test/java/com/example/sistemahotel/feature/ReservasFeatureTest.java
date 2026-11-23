package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Usuario;

// Rutas /reservas/** (HU46 a HU50). Reglas RN01 a RN03 y RN30.
class ReservasFeatureTest extends FeatureTestCase {

    private Usuario recepcion;
    private Cliente cliente;
    private Habitacion q30;
    private Habitacion q31;

    @BeforeEach
    void datos() throws Exception {
        recepcion = crearRecepcionista("qa_recepcion");
        Categoria categoria = crearCategoriaConTemporada("Domo QA", 300, 380, "2027-12-20", "2028-01-05");
        q30 = crearHabitacion("Q30", categoria, 2);
        q31 = crearHabitacion("Q31", categoria, 4);
        cliente = crearCliente("Familia", "Quispe", "88500001");
    }

    private int reservaDelCliente() {
        return columna("SELECT MAX(id) FROM reserva WHERE cliente_id = ?", Integer.class, cliente.getId());
    }

    @Test
    void el_formulario_de_reserva_se_muestra() throws Exception {
        assertOk(actingAs(recepcion).get("/reservas/nueva/"));
    }

    @Test
    void una_reserva_de_dos_habitaciones_crea_una_estadia_por_habitacion() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/reservas/guardar/",
            "cliente", cliente.getId(), "checkIn", "2027-11-10T12:00", "noches", 2,
            "habitaciones", List.of(q30.getId(), q31.getId()), "huespedes_" + q30.getId(), 2, "huespedes_" + q31.getId(), 3);

        int reserva = reservaDelCliente();
        assertRedirect(respuesta, "/reservas/" + reserva + "/");
        assertDatabaseCount("registro", Map.of("reserva_id", reserva, "estado", "ACTIVA"), 2);
        assertDatabaseHas("registro", Map.of("reserva_id", reserva, "habitacion_id", q31.getId(), "num_huespedes", 3, "tarifa", 600.0));
        assertDatabaseHas("habitacion", Map.of("id", q30.getId(), "estado", "OCUPADA"));
        assertDatabaseHas("habitacion", Map.of("id", q31.getId(), "estado", "OCUPADA"));
    }

    @Test
    void en_temporada_alta_la_reserva_cobra_el_precio_alto() throws Exception {
        actingAs(recepcion).post("/reservas/guardar/",
            "cliente", cliente.getId(), "checkIn", "2027-12-26T12:00", "noches", 2, "habitaciones", List.of(q30.getId()));

        assertDatabaseHas("registro", Map.of("reserva_id", reservaDelCliente(), "tarifa", 760.0, "num_huespedes", 1));
    }

    @Test
    void sin_habitaciones_la_reserva_no_se_crea() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/reservas/guardar/", "cliente", cliente.getId(), "checkIn", "2027-11-10T12:00", "noches", 1);

        assertOk(respuesta);
        assertEquals("Elegí al menos una habitación para la reserva.", modelo(respuesta, "error"));
        assertDatabaseMissing("reserva", Map.of("cliente_id", cliente.getId()));
    }

    @Test
    void si_una_habitacion_supera_el_aforo_no_se_crea_ninguna_parte_y_se_conserva_lo_elegido() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/reservas/guardar/",
            "cliente", cliente.getId(), "checkIn", "2027-11-10T12:00", "noches", 2,
            "habitaciones", List.of(q31.getId(), q30.getId()), "huespedes_" + q31.getId(), 2, "huespedes_" + q30.getId(), 5);

        assertTrue(modelo(respuesta, "error").toString().contains("La habitación Q30 tiene aforo para 2"));
        assertEquals(cliente.getId(), modelo(respuesta, "clienteSeleccionadoId"));
        assertEquals(2, ((List<?>) modelo(respuesta, "habitacionesPreseleccionadas")).size());
        assertDatabaseMissing("reserva", Map.of("cliente_id", cliente.getId()));
        assertDatabaseMissing("registro", Map.of("habitacion_id", q31.getId()));
    }

    @Test
    void si_una_habitacion_ya_esta_ocupada_en_esas_fechas_no_se_crea_la_reserva() throws Exception {
        crearEstadia(crearCliente("Otro", "Huésped", "88500002"), q30, fecha("2027-11-09T12:00"), 3, 900, 0, EstadoRegistro.ACTIVA, recepcion);

        MvcResult respuesta = actingAs(recepcion).post("/reservas/guardar/",
            "cliente", cliente.getId(), "checkIn", "2027-11-10T12:00", "noches", 1, "habitaciones", List.of(q31.getId(), q30.getId()));

        assertTrue(modelo(respuesta, "error").toString().contains("ya tiene otra reserva activa"));
        assertDatabaseMissing("registro", Map.of("cliente_id", cliente.getId()));
    }

    @Test
    void una_fecha_invalida_se_avisa_sin_crear_nada() throws Exception {
        MvcResult respuesta = actingAs(recepcion).post("/reservas/guardar/",
            "cliente", cliente.getId(), "checkIn", "mañana", "noches", 1, "habitaciones", List.of(q30.getId()));

        assertEquals("La fecha de ingreso no es válida.", modelo(respuesta, "error"));
        assertDatabaseMissing("reserva", Map.of("cliente_id", cliente.getId()));
    }

    @Test
    void el_detalle_de_la_reserva_muestra_sus_habitaciones() throws Exception {
        actingAs(recepcion).post("/reservas/guardar/",
            "cliente", cliente.getId(), "checkIn", "2027-11-10T12:00", "noches", 1, "habitaciones", List.of(q30.getId(), q31.getId()));

        MvcResult respuesta = get("/reservas/" + reservaDelCliente() + "/");

        assertOk(respuesta);
        assertSee(respuesta, "Q30");
        assertSee(respuesta, "Q31");
    }

    @Test
    void una_reserva_que_no_existe_vuelve_a_la_lista() throws Exception {
        MvcResult respuesta = actingAs(recepcion).get("/reservas/999999/");

        assertTrue(respuesta.getResponse().getRedirectedUrl() != null);
    }
}
