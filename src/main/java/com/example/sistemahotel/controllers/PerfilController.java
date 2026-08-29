package com.example.sistemahotel.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Usuario;

// Autoservicio para el usuario logueado (a diferencia de /usuarios/, que es
// solo para administradores gestionando a otros).
@RequestMapping("/perfil/")
@Controller
public class PerfilController {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String ver(Model model) {
        model.addAttribute("usuario", usuarioActual());
        return "perfil";
    }

    @PostMapping("/cambiar-contrasena/")
    public String cambiarContrasena(
        @RequestParam String actual,
        @RequestParam String nueva,
        @RequestParam String confirmar,
        RedirectAttributes redirectAttributes
    ) {
        Usuario usuario = usuarioActual();

        if (!passwordEncoder.matches(actual, usuario.getContrasena())) {
            redirectAttributes.addFlashAttribute("error", "La contraseña actual no es correcta.");
            return "redirect:/perfil/";
        }
        if (nueva == null || nueva.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "La nueva contraseña no puede estar vacía.");
            return "redirect:/perfil/";
        }
        if (!nueva.equals(confirmar)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas nuevas no coinciden.");
            return "redirect:/perfil/";
        }

        usuario.setContrasena(passwordEncoder.encode(nueva));
        usuarioService.guardar(usuario);
        redirectAttributes.addFlashAttribute("exito", "Contraseña actualizada correctamente.");
        return "redirect:/perfil/";
    }

    private Usuario usuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.consultarpornombreusuario(username)
            .orElseThrow(() -> new IllegalStateException("Usuario logueado no encontrado."));
    }
}
