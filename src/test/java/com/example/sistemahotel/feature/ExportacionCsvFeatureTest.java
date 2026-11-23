package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Usuario;

// Contenido del archivo que descarga "Exportar" en la lista de estadías
// (como AttendanceHistoryExportTest en Laravel): cabecera, columnas, montos,
// formato de fechas, filtros, orden y escape de comillas.
class ExportacionCsvFeatureTest extends FeatureTestCase {

    private static final String CABECERA = "Cliente,Habitacion,Check-in,Checkout estimado,Estado,Tarifa,Saldo pendiente,Registrado por";
    private static final SimpleDateFormat FECHA = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private Usuario recepcion;
    private Categoria categoria;

    @BeforeEach
    void datos() {
        recepcion = crearRecepcionista("qa_recepcion");
        categoria = crearCategoria("Categoría CSV QA", 180);
    }

    private String descargar(Object... filtros) throws Exception {
        MvcResult respuesta = actingAs(recepcion).get("/exportar/", filtros);
        assertTrue(respuesta.getResponse().getContentType().startsWith("text/csv"));
        assertTrue(respuesta.getResponse().getHeader("Content-Disposition").contains("attachment; filename=\"registros.csv\""));
        return respuesta.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private List<String> filas(String csv) {
        return Arrays.asList(csv.replace("﻿", "").split("\\r?\\n"));
    }

    /** Mismo formato numérico que usa la aplicación al escribir el archivo. */
    private String monto(double valor) {
        return String.format("%.2f", valor);
    }

    @Test
    void el_archivo_empieza_con_bom_y_la_cabecera_completa() throws Exception {
        String csv = descargar("q", "Nadie Coincide QA");

        assertEquals('﻿', csv.charAt(0), "Sin BOM Excel no detecta UTF-8");
        assertEquals(List.of(CABECERA), filas(csv));
    }

    @Test
    void cada_columna_trae_el_dato_correcto_de_la_estadia() throws Exception {
        Cliente cliente = crearCliente("Ñusta", "Qacsv Rojas", "89000001");
        Registro r = crearEstadia(cliente, crearHabitacion("QC1", categoria, 2), fecha("2028-05-10T15:30"), 2, 360, 100, EstadoRegistro.ACTIVA, recepcion);
        crearConsumo(r, "Minibar", 20, null);

        List<String> filas = filas(descargar("q", "Qacsv"));

        assertEquals(2, filas.size());
        assertEquals(String.join(",",
            "\"Ñusta Qacsv Rojas\"", "\"QC1\"", "\"10/05/2028 15:30\"", "\"12/05/2028 12:00\"", "\"ACTIVA\"",
            "\"" + monto(360) + "\"", "\"" + monto(280) + "\"", "\"Usuario Prueba\""), filas.get(1));
    }

    @Test
    void filtra_por_estado() throws Exception {
        Cliente cliente = crearCliente("Ana", "Qacsv", "89000002");
        crearEstadia(cliente, crearHabitacion("QC2", categoria, 2), fecha("2028-05-10T12:00"), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);
        crearEstadia(cliente, crearHabitacion("QC3", categoria, 2), fecha("2026-01-10T12:00"), 1, 180, 180, EstadoRegistro.FINALIZADA, recepcion);

        List<String> filas = filas(descargar("q", "Qacsv", "estado", "FINALIZADA"));

        assertEquals(2, filas.size());
        assertTrue(filas.get(1).contains("\"QC3\"") && filas.get(1).contains("\"FINALIZADA\""));
    }

    @Test
    void busca_por_documento_y_por_numero_de_habitacion() throws Exception {
        crearEstadia(crearCliente("Ana", "Qacsv", "89000003"), crearHabitacion("QC4", categoria, 2), fecha("2028-05-10T12:00"), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        assertEquals(2, filas(descargar("q", "89000003")).size());
        assertEquals(2, filas(descargar("q", "QC4")).size());
    }

    @Test
    void sigue_el_orden_de_la_pantalla_vencidas_luego_activas_luego_historial() throws Exception {
        Cliente cliente = crearCliente("Orden", "Qacsv", "89000004");
        crearEstadia(cliente, crearHabitacion("QC5", categoria, 2), fecha("2026-01-05T12:00"), 1, 180, 180, EstadoRegistro.FINALIZADA, recepcion);
        crearEstadia(cliente, crearHabitacion("QC6", categoria, 2), diasDesdeHoy(5, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);
        crearEstadia(cliente, crearHabitacion("QC7", categoria, 2), diasDesdeHoy(-4, 12), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        List<String> filas = filas(descargar("q", "Qacsv"));

        assertEquals(4, filas.size());
        assertTrue(filas.get(1).contains("\"QC7\""), "Primero la vencida");
        assertTrue(filas.get(2).contains("\"QC6\""), "Luego la activa al día");
        assertTrue(filas.get(3).contains("\"QC5\""), "Al final el historial");
    }

    @Test
    void las_comillas_y_comas_del_nombre_no_rompen_las_columnas() throws Exception {
        Cliente cliente = crearCliente("Juan \"Pepe\"", "Qacsv, Jr", "89000005");
        crearEstadia(cliente, crearHabitacion("QC8", categoria, 2), fecha("2028-05-10T12:00"), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        List<String> filas = filas(descargar("q", "89000005"));

        assertTrue(filas.get(1).startsWith("\"Juan \"\"Pepe\"\" Qacsv, Jr\",\"QC8\","), filas.get(1));
    }

    @Test
    void la_fecha_de_check_in_se_escribe_en_formato_peruano() throws Exception {
        Registro r = crearEstadia(crearCliente("Fecha", "Qacsv", "89000006"), crearHabitacion("QC9", categoria, 2),
            fecha("2028-12-31T09:05"), 1, 180, 0, EstadoRegistro.ACTIVA, recepcion);

        String fila = filas(descargar("q", "89000006")).get(1);

        assertTrue(fila.contains("\"" + FECHA.format(r.getCheckIn()) + "\""));
        assertTrue(fila.contains("\"31/12/2028 09:05\""));
        assertTrue(fila.contains("\"01/01/2029 12:00\""));
    }
}
