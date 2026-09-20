package com.example.sistemahotel.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IRegistroService;

@RequestMapping("/consumos/")
@Controller
public class ConsumoController {

    @Autowired
    private IConsumoService consumoService;

    @Autowired
    private IRegistroService registroService;

    @GetMapping("/{registroID}/")
    public String ver(@PathVariable(name = "registroID") int registroId, Model model) {
        return registroService.consultarId(registroId)
            .map(registro -> {
                model.addAttribute("registro", registro);
                model.addAttribute("listaConsumos", consumoService.listarPorRegistro(registroId));
                return "registros/consumos";
            })
            .orElse("redirect:/");
    }

    @PostMapping("/agregar/{registroID}/")
    public String agregar(
        @PathVariable(name = "registroID") int registroId,
        @RequestParam(required = false) String descripcion,
        @RequestParam(required = false) Double monto,
        RedirectAttributes redirectAttributes
    ) {
        if (descripcion == null || descripcion.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "La descripción del consumo es obligatoria.");
            return "redirect:/consumos/" + registroId + "/";
        }
        if (monto == null || monto <= 0) {
            redirectAttributes.addFlashAttribute("error", "El monto debe ser mayor a cero.");
            return "redirect:/consumos/" + registroId + "/";
        }

        registroService.consultarId(registroId)
            .ifPresentOrElse(
                registro -> {
                    consumoService.agregar(registro, descripcion, monto);
                    redirectAttributes.addFlashAttribute("exito", "Consumo agregado correctamente.");
                },
                () -> redirectAttributes.addFlashAttribute("error", "No se encontró el registro.")
            );
        return "redirect:/consumos/" + registroId + "/";
    }

    @PostMapping("/eliminar/{consumoID}/")
    public String eliminar(@PathVariable(name = "consumoID") int consumoId) {
        int registroId = consumoService.eliminar(consumoId);
        return "redirect:/consumos/" + registroId + "/";
    }
}
