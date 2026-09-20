package com.example.sistemahotel.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

// Calendario visual de ocupacion: una fila por habitacion, una columna por dia,
// con una barra por cada estadia que cae dentro del rango visible. Reemplaza
// tener que cruzar fechas mentalmente mirando la lista de registros.
@Controller
@RequestMapping("/ocupacion/")
public class OcupacionController {

    private static final int DIAS_VISIBLES = 14;
    private static final SimpleDateFormat FORMATO_URL = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private IHabitacionService habitacionService;

    @Autowired
    private IRegistroService registroService;

    public static class BloqueOcupacion {
        public final Registro registro;
        public final int colInicio; // 0-indexado dentro del rango visible
        public final int span;      // cantidad de dias que ocupa dentro del rango visible

        public BloqueOcupacion(Registro registro, int colInicio, int span) {
            this.registro = registro;
            this.colInicio = colInicio;
            this.span = span;
        }
    }

    public static class FilaOcupacion {
        public final Habitacion habitacion;
        public final List<BloqueOcupacion> bloques;

        public FilaOcupacion(Habitacion habitacion, List<BloqueOcupacion> bloques) {
            this.habitacion = habitacion;
            this.bloques = bloques;
        }
    }

    @GetMapping("/")
    public String ver(@RequestParam(required = false) String desde, Model model) {
        Date inicio = truncarADia(parsearODefault(desde));
        Date fin = sumarDias(inicio, DIAS_VISIBLES); // exclusivo

        List<Date> dias = new ArrayList<>();
        for (int i = 0; i < DIAS_VISIBLES; i++) {
            dias.add(sumarDias(inicio, i));
        }

        List<Habitacion> habitaciones = habitacionService.listar();

        List<Registro> registrosEnRango = registroService.listar().stream()
            .filter(r -> r.getEstado() == EstadoRegistro.ACTIVA || r.getEstado() == EstadoRegistro.FINALIZADA)
            .filter(r -> r.getHabitacion() != null && r.getCheckIn() != null)
            .filter(r -> seSuperpone(r, inicio, fin))
            .collect(Collectors.toList());

        Map<Integer, List<Registro>> registrosPorHabitacion = registrosEnRango.stream()
            .collect(Collectors.groupingBy(r -> r.getHabitacion().getId()));

        List<FilaOcupacion> filas = new ArrayList<>();
        for (Habitacion habitacion : habitaciones) {
            List<Registro> registrosDelCuarto = registrosPorHabitacion.getOrDefault(habitacion.getId(), List.of());
            List<BloqueOcupacion> bloques = new ArrayList<>();
            for (Registro registro : registrosDelCuarto) {
                Date checkInDia = truncarADia(registro.getCheckIn());
                Date checkOutDiaExclusivo = sumarDias(checkInDia, registro.getNoches());

                Date bloqueInicio = checkInDia.before(inicio) ? inicio : checkInDia;
                Date bloqueFin = checkOutDiaExclusivo.after(fin) ? fin : checkOutDiaExclusivo;

                int colInicio = diasEntre(inicio, bloqueInicio);
                int span = diasEntre(bloqueInicio, bloqueFin);
                if (span > 0) {
                    bloques.add(new BloqueOcupacion(registro, colInicio, span));
                }
            }
            filas.add(new FilaOcupacion(habitacion, bloques));
        }

        model.addAttribute("filas", filas);
        model.addAttribute("dias", dias);
        model.addAttribute("hoy", truncarADia(new Date()));
        model.addAttribute("desdeAnterior", FORMATO_URL.format(sumarDias(inicio, -DIAS_VISIBLES)));
        model.addAttribute("desdeSiguiente", FORMATO_URL.format(fin));
        model.addAttribute("desdeHoy", FORMATO_URL.format(truncarADia(new Date())));

        return "ocupacion";
    }

    private Date parsearODefault(String desde) {
        if (desde != null && !desde.isBlank()) {
            try {
                return FORMATO_URL.parse(desde);
            } catch (ParseException ignored) {
                // Si la fecha no se puede parsear, se usa hoy.
            }
        }
        return new Date();
    }

    private boolean seSuperpone(Registro registro, Date inicioRango, Date finRango) {
        Date checkInDia = truncarADia(registro.getCheckIn());
        Date checkOutDiaExclusivo = sumarDias(checkInDia, registro.getNoches());
        return checkInDia.before(finRango) && checkOutDiaExclusivo.after(inicioRango);
    }

    private Date truncarADia(Date fecha) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date sumarDias(Date fecha, int dias) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);
        calendar.add(Calendar.DAY_OF_MONTH, dias);
        return calendar.getTime();
    }

    private int diasEntre(Date desde, Date hasta) {
        long diffMillis = hasta.getTime() - desde.getTime();
        return (int) Math.round(diffMillis / 86400000.0);
    }
}
