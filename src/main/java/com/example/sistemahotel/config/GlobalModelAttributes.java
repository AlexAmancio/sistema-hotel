package com.example.sistemahotel.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.sistemahotel.interfaces.IUsuarioService;

// Inyecta en TODAS las vistas quién es el usuario logueado y su rol, para
// que la esquina de la barra lateral (navegacion.html) pueda mostrarlo sin
// que cada controlador tenga que agregarlo a mano.
@ControllerAdvice
public class GlobalModelAttributes {

    @Autowired
    private IUsuarioService usuarioService;

    @ModelAttribute
    public void agregarUsuarioActual(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        boolean esAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("usuarioActualRol", esAdmin ? "Administrador" : "Usuario Normal");

        usuarioService.consultarpornombreusuario(auth.getName()).ifPresentOrElse(
            usuario -> model.addAttribute("usuarioActualNombre", usuario.getNombres() + " " + usuario.getApellidos()),
            () -> model.addAttribute("usuarioActualNombre", auth.getName())
        );
    }
}
