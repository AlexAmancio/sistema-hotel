package com.example.sistemahotel.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.IConsumoService;
import com.example.sistemahotel.interfaces.IProductoConsumoService;
import com.example.sistemahotel.modelos.ProductoConsumo;

@RequestMapping("/productos-consumo/")
@Controller
public class ProductosConsumoController {
    String carpeta = "productos-consumo/";

    @Autowired
    private IProductoConsumoService service;

    @Autowired
    private IConsumoService consumoService;

    @GetMapping("/")
    public String listar(Model model) {
        model.addAttribute("listaProductos", service.listar());
        // Cualquier cuenta autenticada puede ver el catálogo (lo necesita para
        // cargar consumos), pero el template usa este flag para ocultar
        // crear/editar/eliminar/precio al staff sin permisos de administrador
        // (el backend igual los bloquea en SecurityConfig; esto es solo para
        // no mostrar acciones que van a terminar en un 403).
        model.addAttribute("esAdmin", esAdminActual());
        return carpeta + "producto-consumo-lista";
    }

    private boolean esAdminActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/nuevo/")
    public String nuevo(Model model) {
        model.addAttribute("producto", new ProductoConsumo());
        model.addAttribute("titulo", "Nuevo producto de consumo");
        model.addAttribute("txtboton", "Agregar");
        return carpeta + "producto-consumo-form";
    }

    @GetMapping("/editar/{productoID}/")
    public String editar(@PathVariable(name = "productoID") int id, Model model) {
        model.addAttribute("producto", service.consultarId(id).orElse(new ProductoConsumo()));
        model.addAttribute("titulo", "Editar producto de consumo");
        model.addAttribute("txtboton", "Actualizar");
        return carpeta + "producto-consumo-form";
    }

    @PostMapping("/guardar/")
    public String guardar(@ModelAttribute("producto") ProductoConsumo producto, RedirectAttributes redirectAttributes) {
        String redirectError = producto.getId() < 1
            ? "redirect:/productos-consumo/nuevo/"
            : "redirect:/productos-consumo/editar/" + producto.getId() + "/";

        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre es obligatorio.");
            return redirectError;
        }
        if (producto.getPrecioSugerido() < 0) {
            redirectAttributes.addFlashAttribute("error", "El precio sugerido no puede ser negativo.");
            return redirectError;
        }

        boolean esNuevo = producto.getId() < 1;
        service.guardar(producto);
        redirectAttributes.addFlashAttribute("exito",
            esNuevo ? "Producto creado correctamente." : "Producto actualizado correctamente.");
        return "redirect:/productos-consumo/";
    }

    @PostMapping("/desactivar/{productoID}/")
    public String desactivar(@PathVariable(name = "productoID") int id, RedirectAttributes redirectAttributes) {
        service.consultarId(id).ifPresent(producto -> {
            producto.setActivo(false);
            service.guardar(producto);
        });
        redirectAttributes.addFlashAttribute("exito", "Producto desactivado — ya no aparece como atajo al cargar consumos.");
        return "redirect:/productos-consumo/";
    }

    @PostMapping("/activar/{productoID}/")
    public String activar(@PathVariable(name = "productoID") int id, RedirectAttributes redirectAttributes) {
        service.consultarId(id).ifPresent(producto -> {
            producto.setActivo(true);
            service.guardar(producto);
        });
        redirectAttributes.addFlashAttribute("exito", "Producto reactivado.");
        return "redirect:/productos-consumo/";
    }

    @PostMapping("/eliminar/{productoID}/")
    public String eliminar(@PathVariable(name = "productoID") int id, RedirectAttributes redirectAttributes) {
        boolean enUso = consumoService.existeConsumoConProducto(id);
        if (enUso) {
            redirectAttributes.addFlashAttribute("error",
                "No se puede eliminar: hay consumos ya cobrados que usan este producto. Desactivalo en vez de eliminarlo.");
            return "redirect:/productos-consumo/";
        }
        service.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Producto eliminado correctamente.");
        return "redirect:/productos-consumo/";
    }
}
