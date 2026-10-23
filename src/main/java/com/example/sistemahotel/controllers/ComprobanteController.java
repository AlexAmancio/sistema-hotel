package com.example.sistemahotel.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.IComprobanteService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.EstadoRegistro;

@RequestMapping("/comprobantes/")
@Controller
public class ComprobanteController {

    @Autowired
    private IComprobanteService comprobanteService;

    @Autowired
    private IRegistroService registroService;

    @PostMapping("/emitir/{registroID}/")
    public String emitir(@PathVariable(name = "registroID") int registroId, RedirectAttributes redirectAttributes) {
        return registroService.consultarId(registroId)
            .map(registro -> {
                // El comprobante congela el total (tarifa + consumos) del
                // momento en que se emite y no se vuelve a recalcular. Emitirlo
                // antes del check-out deja afuera cualquier consumo que se
                // cargue después — el total quedaría mal para siempre.
                if (registro.getEstado() != EstadoRegistro.FINALIZADA) {
                    redirectAttributes.addFlashAttribute("error",
                        "No se puede emitir comprobante: la estadía todavía no tiene check-out hecho.");
                    return "redirect:/";
                }
                comprobanteService.emitir(registro);
                return "redirect:/comprobantes/ver/" + registroId + "/";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "No se encontró el registro para emitir el comprobante.");
                return "redirect:/";
            });
    }

    @GetMapping("/ver/{registroID}/")
    public String ver(@PathVariable(name = "registroID") int registroId, Model model) {
        Comprobante comprobante = comprobanteService.consultarPorRegistro(registroId).orElse(null);
        if (comprobante == null) {
            return "redirect:/";
        }
        model.addAttribute("comprobante", comprobante);
        model.addAttribute("registro", comprobante.getRegistro());
        return "comprobantes/comprobante-ver";
    }
}
