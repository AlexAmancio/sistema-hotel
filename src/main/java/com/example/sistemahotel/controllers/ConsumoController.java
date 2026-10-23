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
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Registro;

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
                    // Defensa en profundidad: el formulario ya se oculta en el HTML
                    // cuando la estadía no está activa, pero eso no evita un POST
                    // directo a esta ruta.
                    if (registro.getEstado() != EstadoRegistro.ACTIVA) {
                        redirectAttributes.addFlashAttribute("error",
                            "Esta estadía ya no está activa — no se pueden agregar más consumos.");
                        return;
                    }
                    consumoService.agregar(registro, descripcion, monto);
                    redirectAttributes.addFlashAttribute("exito", "Consumo agregado correctamente.");
                },
                () -> redirectAttributes.addFlashAttribute("error", "No se encontró el registro.")
            );
        return "redirect:/consumos/" + registroId + "/";
    }

    @PostMapping("/eliminar/{registroID}/{consumoID}/")
    public String eliminar(
        @PathVariable(name = "registroID") int registroId,
        @PathVariable(name = "consumoID") int consumoId,
        RedirectAttributes redirectAttributes
    ) {
        Registro registro = registroService.consultarId(registroId).orElse(null);
        if (registro == null) {
            return "redirect:/";
        }
        // Misma razón que en agregar(): el botón ya se oculta en el HTML si la
        // estadía no está activa, pero eso no evita un POST directo a esta ruta.
        if (registro.getEstado() != EstadoRegistro.ACTIVA) {
            redirectAttributes.addFlashAttribute("error",
                "Esta estadía ya no está activa — no se pueden quitar consumos de su historial.");
            return "redirect:/consumos/" + registroId + "/";
        }
        consumoService.eliminar(consumoId);
        return "redirect:/consumos/" + registroId + "/";
    }
}
