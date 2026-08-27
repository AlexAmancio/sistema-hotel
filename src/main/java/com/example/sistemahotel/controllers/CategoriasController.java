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
import com.example.sistemahotel.modelos.Categoria;

@RequestMapping("/categorias/")
@Controller
public class CategoriasController {
    String carpeta = "categorias/";
    
    @Autowired
    private ICategoriaService service;

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
    public String guardar(@ModelAttribute("categoria") Categoria categoria, Model model,RedirectAttributes redirectAttributes) {
        if(categoria.getId()<1){
            Optional<Categoria> categoriaexistente = service.consultarPorNombrecategoria(categoria.getNombre());
            if(categoriaexistente.isPresent()){
                redirectAttributes.addFlashAttribute("error", "No se pudo registrar la categoria. N° de categoria repetido");
                return "redirect:/categorias/nuevo/";
            }
            service.guardar(categoria);
            return listar(model);
        }
        service.guardar(categoria);
        return listar(model);
    }

    @PostMapping("/eliminar/{categoriaID}/")
    public String eliminar(
        @PathVariable(name="categoriaID", required=true) int id,
        Model model
    ) {
        service.eliminar(id);
        return listar(model);
    }
}
