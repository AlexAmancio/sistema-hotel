package com.example.sistemahotel.controllers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.util.FechaUtils;

// "Reportes": lo que antes eran dos pantallas separadas (Dashboard con las
// metricas de hoy, y Reportes con ingresos/ocupacion por mes) ahora es una
// sola. /dashboard/ se mantiene como alias por compatibilidad, pero el link
// de la barra lateral y el nombre visible son "Reportes".
@Controller
public class DashboardController {

    private static final int MESES_VISIBLES = 6;
    private static final String[] NOMBRES_MES = {
        "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    @Autowired
    private IRegistroService registroService;

    @Autowired
    private IHabitacionService habitacionService;

    public static class FilaReporte {
        public final String mes;
        public final double ingresos;
        public final Double variacionPct; // null si no hay mes anterior con el que comparar
        public final double ocupacionPromedio; // 0-100

        public FilaReporte(String mes, double ingresos, Double variacionPct, double ocupacionPromedio) {
            this.mes = mes;
            this.ingresos = ingresos;
            this.variacionPct = variacionPct;
            this.ocupacionPromedio = ocupacionPromedio;
        }
    }

    @GetMapping({"/dashboard/", "/reportes/"})
    public String dashboard(Model model) {
        List<Habitacion> habitaciones = habitacionService.listar();
        List<Registro> registros = registroService.listar();
        Date hoy = new Date();

        long totalHabitaciones = habitaciones.size();
        long ocupadas = habitaciones.stream().filter(h -> h.getEstado() == EstadoHabitacion.OCUPADA).count();
        long disponibles = habitaciones.stream().filter(h -> h.getEstado() == EstadoHabitacion.DISPONIBLE).count();
        long enLimpieza = habitaciones.stream().filter(h -> h.getEstado() == EstadoHabitacion.LIMPIEZA).count();
        long enMantenimiento = habitaciones.stream().filter(h -> h.getEstado() == EstadoHabitacion.MANTENIMIENTO).count();
        int porcentajeOcupacion = totalHabitaciones == 0 ? 0 : (int) Math.round(ocupadas * 100.0 / totalHabitaciones);

        List<Registro> activos = registros.stream()
            .filter(r -> r.getEstado() == EstadoRegistro.ACTIVA)
            .toList();

        long checkInsHoy = registros.stream()
            .filter(r -> r.getEstado() == EstadoRegistro.ACTIVA || r.getEstado() == EstadoRegistro.FINALIZADA)
            .filter(r -> FechaUtils.mismoDia(r.getCheckIn(), hoy))
            .count();

        long checkOutsPendientesHoy = activos.stream()
            .filter(r -> FechaUtils.mismoDia(r.calcularCheckoutEstimado(), hoy))
            .count();

        long vencidas = activos.stream()
            .filter(r -> r.calcularCheckoutEstimadoAmigable().equals("Vencido"))
            .count();

        double ingresosHoy = registros.stream()
            .filter(r -> r.getEstado() == EstadoRegistro.ACTIVA || r.getEstado() == EstadoRegistro.FINALIZADA)
            .filter(r -> FechaUtils.mismoDia(r.getCheckIn(), hoy))
            .mapToDouble(r -> r.getMontoPagado() != null ? r.getMontoPagado() : 0.0)
            .sum();

        double saldoPendienteTotal = activos.stream()
            .mapToDouble(Registro::getSaldoPendiente)
            .sum();

        model.addAttribute("totalHabitaciones", totalHabitaciones);
        model.addAttribute("ocupadas", ocupadas);
        model.addAttribute("disponibles", disponibles);
        model.addAttribute("enLimpieza", enLimpieza);
        model.addAttribute("enMantenimiento", enMantenimiento);
        model.addAttribute("porcentajeOcupacion", porcentajeOcupacion);
        model.addAttribute("checkInsHoy", checkInsHoy);
        model.addAttribute("checkOutsPendientesHoy", checkOutsPendientesHoy);
        model.addAttribute("vencidas", vencidas);
        model.addAttribute("ingresosHoy", ingresosHoy);
        model.addAttribute("saldoPendienteTotal", saldoPendienteTotal);

        agregarReportePorPeriodo(model, registros, (int) totalHabitaciones, hoy);

        return "reportes";
    }

    private void agregarReportePorPeriodo(Model model, List<Registro> registros, int totalHabitaciones, Date hoy) {
        List<FilaReporte> filas = new ArrayList<>();
        Double ingresoMesAnterior = null;

        for (int i = MESES_VISIBLES - 1; i >= 0; i--) {
            Calendar inicioMes = Calendar.getInstance();
            inicioMes.add(Calendar.MONTH, -i);
            inicioMes.set(Calendar.DAY_OF_MONTH, 1);
            ponerAMedianoche(inicioMes);

            Calendar finMes = (Calendar) inicioMes.clone();
            finMes.add(Calendar.MONTH, 1); // exclusivo

            // Si el mes incluye hoy (es el mes actual), solo se promedia la
            // ocupacion hasta hoy: contar dias futuros sin reservas todavia
            // hechas deflacionaria el promedio de un mes que ni termino.
            Calendar limite = Calendar.getInstance();
            limite.setTime(hoy);
            ponerAMedianoche(limite);
            limite.add(Calendar.DAY_OF_MONTH, 1); // el dia de hoy cuenta completo
            Date finEfectivo = limite.getTime().before(finMes.getTime()) ? limite.getTime() : finMes.getTime();

            double ingresos = 0;
            for (Registro r : registros) {
                if (r.getEstado() != EstadoRegistro.ACTIVA && r.getEstado() != EstadoRegistro.FINALIZADA) {
                    continue;
                }
                if (r.getCheckIn() == null || r.getCheckIn().before(inicioMes.getTime()) || !r.getCheckIn().before(finMes.getTime())) {
                    continue;
                }
                ingresos += r.getMontoPagado() != null ? r.getMontoPagado() : 0.0;
            }

            double ocupacionPromedio = 0;
            if (totalHabitaciones > 0 && finEfectivo.after(inicioMes.getTime())) {
                int diasContados = 0;
                double sumaOcupadas = 0;
                Calendar cursor = (Calendar) inicioMes.clone();
                while (cursor.getTime().before(finEfectivo)) {
                    Date diaInicio = cursor.getTime();
                    Calendar diaFinCal = (Calendar) cursor.clone();
                    diaFinCal.add(Calendar.DAY_OF_MONTH, 1);
                    Date diaFin = diaFinCal.getTime();

                    long ocupadasEseDia = registros.stream()
                        .filter(r -> r.getEstado() == EstadoRegistro.ACTIVA || r.getEstado() == EstadoRegistro.FINALIZADA)
                        .filter(r -> r.getHabitacion() != null && r.getCheckIn() != null)
                        .filter(r -> seSuperpone(r, diaInicio, diaFin))
                        .map(r -> r.getHabitacion().getId())
                        .distinct()
                        .count();

                    sumaOcupadas += ocupadasEseDia;
                    diasContados++;
                    cursor.add(Calendar.DAY_OF_MONTH, 1);
                }
                if (diasContados > 0) {
                    ocupacionPromedio = (sumaOcupadas / diasContados) / totalHabitaciones * 100.0;
                }
            }

            Double variacion = ingresoMesAnterior == null || ingresoMesAnterior == 0
                ? null
                : (ingresos - ingresoMesAnterior) / ingresoMesAnterior * 100.0;

            String nombreMes = NOMBRES_MES[inicioMes.get(Calendar.MONTH)] + " " + inicioMes.get(Calendar.YEAR);
            filas.add(new FilaReporte(nombreMes, ingresos, variacion, ocupacionPromedio));

            ingresoMesAnterior = ingresos;
        }

        FilaReporte mesActual = filas.get(filas.size() - 1);
        double ocupacionTrimestre = filas.subList(Math.max(0, filas.size() - 3), filas.size()).stream()
            .mapToDouble(f -> f.ocupacionPromedio)
            .average()
            .orElse(0);

        model.addAttribute("filas", filas);
        model.addAttribute("mesActual", mesActual);
        model.addAttribute("ocupacionTrimestre", ocupacionTrimestre);
    }

    private void ponerAMedianoche(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean seSuperpone(Registro registro, Date inicioRango, Date finRango) {
        Calendar checkInDia = Calendar.getInstance();
        checkInDia.setTime(registro.getCheckIn());
        ponerAMedianoche(checkInDia);
        Calendar checkOutDia = (Calendar) checkInDia.clone();
        checkOutDia.add(Calendar.DAY_OF_MONTH, registro.getNoches());
        return checkInDia.getTime().before(finRango) && checkOutDia.getTime().after(inicioRango);
    }
}
