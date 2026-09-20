package com.example.sistemahotel.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;

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

        String query = q != null ? q.trim() : "";
        if (query.isEmpty()) {
            return "busqueda";
        }

        // El filtrado lo hace la base de datos (WHERE ... LIKE), no el código Java:
        // antes esto traía TODOS los clientes/habitaciones/registros a memoria en
        // cada búsqueda, sin importar cuántos coincidieran.
        model.addAttribute("clientes", clienteService.buscar(query));
        model.addAttribute("habitaciones", habitacionService.buscar(query));
        model.addAttribute("registros", registroService.buscarPorNumeroHabitacion(query));

        return "busqueda";
    }
}
