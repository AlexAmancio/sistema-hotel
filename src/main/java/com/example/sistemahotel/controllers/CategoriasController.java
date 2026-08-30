package com.example.sistemahotel.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.ICategoriaService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.modelos.Categoria;

@RequestMapping("/categorias/")
@Controller
public class CategoriasController {
    String carpeta = "categorias/";

    @Autowired
    private ICategoriaService service;

    @Autowired
    private IHabitacionService habitacionService;

    @GetMapping("/")
    public String listar(Model model) {

        model.addAttribute("listaCategorias", service.listar());

        return carpeta + "categoria-lista";
    }

    @GetMapping("/nuevo/")
    public String nuevo(Model model) {
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("titulo", "Nueva categoría");
        model.addAttribute("txtboton", "Agregar");

        return carpeta + "categoria-form";
    }

    @GetMapping("/editar/{categoriaID}/")
    public String editar(
        @PathVariable(name="categoriaID", required=true) int id,
        Model model
    ) {
        model.addAttribute("categoria", service.consultarId(id));
        model.addAttribute("titulo", "Editar categoría");
        model.addAttribute("txtboton", "Actualizar");
        return carpeta + "categoria-form";
    }

    @PostMapping("/guardar/")
    public String guardar(@ModelAttribute("categoria") Categoria categoria, RedirectAttributes redirectAttributes) {
        String redirectError = categoria.getId() < 1
            ? "redirect:/categorias/nuevo/"
            : "redirect:/categorias/editar/" + categoria.getId() + "/";

        if (categoria.getPrecio() <= 0) {
            redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor a cero.");
            return redirectError;
        }
        if (categoria.getTemporadaAltaInicio() != null && categoria.getTemporadaAltaFin() != null
                && categoria.getTemporadaAltaFin().before(categoria.getTemporadaAltaInicio())) {
            redirectAttributes.addFlashAttribute("error", "La fecha de fin de temporada alta debe ser posterior a la de inicio.");
            return redirectError;
        }

        Optional<Categoria> categoriaExistente = service.consultarPorNombrecategoria(categoria.getNombre());
        boolean nombreRepetido = categoriaExistente.isPresent() && categoriaExistente.get().getId() != categoria.getId();
        if (nombreRepetido) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar la categoría. Nombre repetido.");
            return redirectError;
        }

        boolean esNueva = categoria.getId() < 1;
        service.guardar(categoria);
        redirectAttributes.addFlashAttribute("exito", esNueva ? "Categoría creada correctamente." : "Categoría actualizada correctamente.");
        return "redirect:/categorias/";
    }

    @PostMapping("/eliminar/{categoriaID}/")
    public String eliminar(
        @PathVariable(name="categoriaID", required=true) int id,
        RedirectAttributes redirectAttributes
    ) {
        boolean enUso = habitacionService.listar().stream()
            .anyMatch(h -> h.getCategoria() != null && h.getCategoria().getId() == id);
        if (enUso) {
            redirectAttributes.addFlashAttribute("error",
                "No se puede eliminar: hay habitaciones que usan esta categoría.");
            return "redirect:/categorias/";
        }

        service.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Categoría eliminada correctamente.");
        return "redirect:/categorias/";
    }
}
