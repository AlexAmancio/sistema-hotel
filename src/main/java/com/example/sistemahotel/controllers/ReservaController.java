package com.example.sistemahotel.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        RedirectAttributes redirectAttributes
    ) {
        try {
            Date checkInDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(checkIn);
            var clienteObj = clienteService.consultarId(cliente)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            var usuarioObj = usuarioService.consultarpornombreusuario(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

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

            var reserva = reservaService.crear(clienteObj, usuarioObj, checkInDate, noches, huespedesPorHabitacion, habitacionIds);
            return "redirect:/reservas/" + reserva.getId() + "/";
        } catch (ParseException e) {
            redirectAttributes.addFlashAttribute("error", "La fecha de ingreso no es válida.");
            return "redirect:/reservas/nueva/";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservas/nueva/";
        }
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
