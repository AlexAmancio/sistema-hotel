package com.example.sistemahotel.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.sistemahotel.interfaces.IPasswordResetService;
import com.example.sistemahotel.servicios.PasswordResetService;

@Controller
public class LoginController {

    @Autowired
    private IPasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/login/olvide-contrasena")
    public String olvideContrasena() {
        return "login/olvide-contrasena";
    }

    @PostMapping("/login/olvide-contrasena")
    public String enviarReseteo(@RequestParam String correo, Model model) {
        passwordResetService.solicitarReseteo(correo);
        model.addAttribute("enviado", true);
        return "login/olvide-contrasena";
    }

    @GetMapping("/login/restablecer")
    public String restablecer(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("estado", passwordResetService.tokenValido(token) ? "formulario" : "invalido");
        return "login/restablecer";
    }

    @PostMapping("/login/restablecer")
    public String guardarNuevaContrasena(@RequestParam String token, @RequestParam String nuevaContrasena, Model model) {
        model.addAttribute("token", token);
        // Defecto D12 (regla RN36): la contraseña nueva debe tener al menos 5
        // caracteres. Antes solo lo exigía el formulario (minlength="5").
        if (nuevaContrasena == null || nuevaContrasena.trim().length() < PasswordResetService.LONGITUD_MINIMA) {
            model.addAttribute("estado", passwordResetService.tokenValido(token) ? "formulario" : "invalido");
            model.addAttribute("error", "La contraseña debe tener al menos " + PasswordResetService.LONGITUD_MINIMA + " caracteres.");
            return "login/restablecer";
        }
        boolean actualizado = passwordResetService.restablecerContrasena(token, nuevaContrasena);
        model.addAttribute("estado", actualizado ? "exito" : "invalido");
        return "login/restablecer";
    }
}
