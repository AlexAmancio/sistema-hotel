package com.example.sistemahotel.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IReservaService;
import com.example.sistemahotel.interfaces.IUsuarioService;

@RequestMapping("/reservas/")
@Controller
public class ReservaController {

    @Autowired
    private IReservaService reservaService;

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private IUsuarioService usuarioService;

    @GetMapping("/nueva/")
    public String nueva(Model model) {
        model.addAttribute("listaClientes", clienteService.listar());
        return "reservas/reserva-form";
    }

    @PostMapping("/guardar/")
    public String guardar(
        @RequestParam int cliente,
        @RequestParam String checkIn,
        @RequestParam int noches,
        @RequestParam(name = "habitaciones", required = false) List<Integer> habitacionIds,
        @RequestParam Map<String, String> params,
        Model model
    ) {
        // Cada habitación manda su propio "huespedes_<id>" (ver reserva-form.html);
        // si por algo no llega, se asume 1 huésped en esa habitación.
        Map<Integer, Integer> huespedesPorHabitacion = new HashMap<>();
        if (habitacionIds != null) {
            for (Integer id : habitacionIds) {
                String raw = params.get("huespedes_" + id);
                int huespedes = 1;
                if (raw != null && !raw.isBlank()) {
                    try {
                        huespedes = Integer.parseInt(raw);
                    } catch (NumberFormatException ignored) {
                        // Se queda en 1 si llega algo no numérico.
                    }
                }
                huespedesPorHabitacion.put(id, huespedes);
            }
        }

        try {
            Date checkInDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(checkIn);
            var clienteObj = clienteService.consultarId(cliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            var usuarioObj = usuarioService.consultarpornombreusuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

            var reserva = reservaService.crear(clienteObj, usuarioObj, checkInDate, noches, huespedesPorHabitacion, habitacionIds);
            return "redirect:/reservas/" + reserva.getId() + "/";
        } catch (ParseException e) {
            return repoblarFormularioConError(model, "La fecha de ingreso no es válida.", cliente, checkIn, noches, habitacionIds, huespedesPorHabitacion);
        } catch (IllegalArgumentException e) {
            return repoblarFormularioConError(model, e.getMessage(), cliente, checkIn, noches, habitacionIds, huespedesPorHabitacion);
        }
    }

    // Antes, cualquier error (una sola habitación con aforo superado, o que
    // alguien más la reservó mientras se llenaba el formulario) redirigía a
    // /reservas/nueva/ en blanco: perdías el cliente, las fechas y TODAS las
    // habitaciones que ya habías marcado, no solo la que falló. Para una
    // reserva de varios cuartos eso es particularmente doloroso. Acá se
    // vuelve a renderizar el mismo formulario con todo lo ya elegido, para
    // que el JS lo precargue en vez de arrancar de cero.
    private String repoblarFormularioConError(
        Model model,
        String error,
        int clienteId,
        String checkIn,
        int noches,
        List<Integer> habitacionIds,
        Map<Integer, Integer> huespedesPorHabitacion
    ) {
        model.addAttribute("listaClientes", clienteService.listar());
        model.addAttribute("error", error);
        model.addAttribute("checkInValor", checkIn);
        model.addAttribute("nochesValor", noches);

        clienteService.consultarId(clienteId).ifPresent(c -> {
            model.addAttribute("clienteSeleccionadoId", c.getId());
            model.addAttribute("clienteSeleccionadoNombre", c.getNombres() + " " + c.getApellidos());
        });

        List<Map<String, Integer>> habitacionesPreseleccionadas = new ArrayList<>();
        if (habitacionIds != null) {
            for (Integer id : habitacionIds) {
                Map<String, Integer> item = new HashMap<>();
                item.put("id", id);
                item.put("huespedes", huespedesPorHabitacion.getOrDefault(id, 1));
                habitacionesPreseleccionadas.add(item);
            }
        }
        model.addAttribute("habitacionesPreseleccionadas", habitacionesPreseleccionadas);

        return "reservas/reserva-form";
    }

    @GetMapping("/{reservaID}/")
    public String ver(@PathVariable(name = "reservaID") int id, Model model) {
        return reservaService.consultarId(id)
            .map(reserva -> {
                model.addAttribute("reserva", reserva);
                return "reservas/reserva-ver";
            })
            .orElse("redirect:/");
    }
}
