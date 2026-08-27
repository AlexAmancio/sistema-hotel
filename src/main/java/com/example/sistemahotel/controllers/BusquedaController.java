package com.example.sistemahotel.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

@Controller
public class BusquedaController {

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private IHabitacionService habitacionService;

    @Autowired
    private IRegistroService registroService;

    @GetMapping("/buscar")
    public String buscar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("query", q);

        String query = q != null ? q.trim().toLowerCase() : "";
        if (query.isEmpty()) {
            return "busqueda";
        }

        List<Cliente> clientes = clienteService.listar().stream()
            .filter(c -> contiene(c.getNombres(), query)
                || contiene(c.getApellidos(), query)
                || contiene(c.getIdentificacion(), query))
            .collect(Collectors.toList());

        List<Habitacion> habitaciones = habitacionService.listar().stream()
            .filter(h -> contiene(h.getNumero(), query))
            .collect(Collectors.toList());

        List<Registro> registros = registroService.listar().stream()
            .filter(r -> r.getHabitacion() != null && contiene(r.getHabitacion().getNumero(), query))
            .collect(Collectors.toList());

        model.addAttribute("clientes", clientes);
        model.addAttribute("habitaciones", habitaciones);
        model.addAttribute("registros", registros);

        return "busqueda";
    }

    private boolean contiene(String campo, String query) {
        return campo != null && campo.toLowerCase().contains(query);
    }
}
