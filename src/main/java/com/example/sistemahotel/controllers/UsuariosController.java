package com.example.sistemahotel.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.util.TextoUtils;


@RequestMapping("/usuarios/")
@Controller
public class UsuariosController {
    String carpeta = "usuarios/";

    @Autowired
    private IUsuarioService service;

    @Autowired
    private IRegistroService registroService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String listar(Model model) {
        model.addAttribute("listaUsuarios", service.listar());
        // El staff sin permisos de administrador puede ver esta lista, pero el
        // template usa este flag para ocultar los botones de crear/editar/eliminar
        // (el backend igual los bloquea en SecurityConfig; esto es solo para no
        // mostrar acciones que van a terminar en un 403).
        model.addAttribute("esAdmin", esAdminActual());

        return carpeta + "usuario-lista";
    }

    private boolean esAdminActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/nuevo/")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("titulo", "Nuevo usuario");
        model.addAttribute("txtboton", "Agregar");

        return carpeta + "usuario-form";
    }

    @GetMapping("/editar/{usuarioID}/")
    public String editar(
        @PathVariable(name="usuarioID", required=true) int id,
        Model model
    ) {
        model.addAttribute("usuario", service.consultarId(id));
        model.addAttribute("titulo", "Editar usuario");
        model.addAttribute("txtboton", "Actualizar");

        return carpeta + "usuario-form";
    }

    @PostMapping("/guardar/")
    public String guardar(@ModelAttribute("usuario") Usuario usuario, Model model, RedirectAttributes redirectAttributes) {
        // Igual que en Clientes: nombres y apellidos con inicial mayúscula,
        // sin tocar username ni correo.
        usuario.setNombres(TextoUtils.capitalizarNombre(usuario.getNombres()));
        usuario.setApellidos(TextoUtils.capitalizarNombre(usuario.getApellidos()));

        if(usuario.getId()<1){
            if(usuario.getContrasena() == null || usuario.getContrasena().isBlank()){
                redirectAttributes.addFlashAttribute("error", "La contraseña es obligatoria para un nuevo usuario");
                return "redirect:/usuarios/nuevo/";
            }
            Optional<Usuario> usuarioexistente = service.consultarpornombreusuario(usuario.getUsername());
            if(usuarioexistente.isPresent()){
                redirectAttributes.addFlashAttribute("error", "No se pudo registrar al usuario, username repetido");
                return "redirect:/usuarios/nuevo/";
            }
            // Encriptar contraseña del usuario
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
            // Guardar usuario
            service.guardar(usuario);
            model.addAttribute("exito", "Usuario creado correctamente.");
            return listar(model);
        }
        // Edición: el campo de contraseña llega vacío salvo que el admin quiera cambiarla.
        // Si llega vacío, se conserva el hash ya guardado en vez de volver a encriptarlo
        // (encriptar un hash ya encriptado dejaba al usuario sin poder loguearse).
        if(usuario.getContrasena() == null || usuario.getContrasena().isBlank()){
            service.consultarId(usuario.getId()).ifPresent(existente -> usuario.setContrasena(existente.getContrasena()));
        } else {
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        }
        // Guardar usuario
        service.guardar(usuario);
        model.addAttribute("exito", "Usuario actualizado correctamente.");
        return listar(model);
    }

    @PostMapping("/eliminar/{usuarioID}/")
    public String eliminar(
        @PathVariable(name="usuarioID", required = true) int id,
        RedirectAttributes redirectAttributes
    ) {
        Optional<Usuario> usuario = service.consultarId(id);
        if (usuario.isEmpty()) {
            return "redirect:/usuarios/";
        }

        if (usuario.get().isEsAdmin() && esElUnicoAdmin(id)) {
            redirectAttributes.addFlashAttribute("error", "No se puede eliminar al único administrador del sistema.");
            return "redirect:/usuarios/";
        }

        boolean tieneRegistros = registroService.listar().stream()
            .anyMatch(r -> r.getUsuario() != null && r.getUsuario().getId() == id);
        if (tieneRegistros) {
            redirectAttributes.addFlashAttribute("error",
                "No se puede eliminar: este usuario tiene registros asociados. Desactivalo en su lugar.");
            return "redirect:/usuarios/";
        }

        service.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Usuario eliminado correctamente.");
        return "redirect:/usuarios/";
    }

    @PostMapping("/desactivar/{usuarioID}/")
    public String desactivar(
        @PathVariable(name="usuarioID", required = true) int id,
        RedirectAttributes redirectAttributes
    ) {
        Optional<Usuario> usuarioOpt = service.consultarId(id);
        if (usuarioOpt.isEmpty()) {
            return "redirect:/usuarios/";
        }
        Usuario usuario = usuarioOpt.get();

        if (usuario.isEsAdmin() && esElUnicoAdmin(id)) {
            redirectAttributes.addFlashAttribute("error", "No se puede desactivar al único administrador del sistema.");
            return "redirect:/usuarios/";
        }

        usuario.setActivo(false);
        service.guardar(usuario);
        redirectAttributes.addFlashAttribute("exito", "Usuario desactivado correctamente.");
        return "redirect:/usuarios/";
    }

    @PostMapping("/activar/{usuarioID}/")
    public String activar(
        @PathVariable(name="usuarioID", required = true) int id,
        RedirectAttributes redirectAttributes
    ) {
        service.consultarId(id).ifPresent(usuario -> {
            usuario.setActivo(true);
            service.guardar(usuario);
        });
        redirectAttributes.addFlashAttribute("exito", "Usuario activado correctamente.");
        return "redirect:/usuarios/";
    }

    // True si, de todos los administradores ACTIVOS, el único es el que tiene este id
    // (o si ya no queda ninguno activo aparte de él).
    private boolean esElUnicoAdmin(int id) {
        List<Usuario> admins = service.listar().stream()
            .filter(Usuario::isEsAdmin)
            .filter(Usuario::isActivo)
            .toList();
        return admins.size() <= 1 && admins.stream().anyMatch(a -> a.getId() == id);
    }
}
