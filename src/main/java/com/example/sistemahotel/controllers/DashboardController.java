package com.example.sistemahotel.controllers;

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

@Controller
public class DashboardController {

    @Autowired
    private IRegistroService registroService;

    @Autowired
    private IHabitacionService habitacionService;

    @GetMapping("/dashboard/")
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

        return "dashboard";
    }
}
