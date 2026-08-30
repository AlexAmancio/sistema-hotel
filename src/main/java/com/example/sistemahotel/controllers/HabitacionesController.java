package com.example.sistemahotel.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.text.ParseException;
import com.example.sistemahotel.interfaces.ICategoriaService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.servicios.RegistroService;


@RequestMapping("/habitaciones/")
@Controller
public class HabitacionesController {
    String carpeta = "habitaciones/";
    
    @Autowired
    private IHabitacionService service;

    @Autowired
    private ICategoriaService categoriaService;
    @Autowired
    private RegistroService registroService;
    
    @GetMapping("/")
    public String listar(Model model) {
        model.addAttribute("listaHabitaciones", service.listar());

        return carpeta + "habitacion-lista";
    }

    @GetMapping("/nuevo/")
    public String nuevo(Model model) {
        model.addAttribute("habitacion", new Habitacion());
        model.addAttribute("listaCategorias", categoriaService.listar());
        model.addAttribute("titulo", "Nueva habitación");
        model.addAttribute("txtboton", "Agregar");

        return carpeta + "habitacion-form";
    }

    @GetMapping("/editar/{habitacionID}/")
    public String editar(
        @PathVariable(name="habitacionID", required=true) int id,
        Model model
    ) {
        model.addAttribute("habitacion", service.consultarId(id));
        model.addAttribute("listaCategorias", categoriaService.listar());

        model.addAttribute("titulo", "Editar habitación");
        model.addAttribute("txtboton", "Actualizar");

        return carpeta + "habitacion-form";
    }

    @PostMapping("/guardar/")
    public String guardar(@ModelAttribute("habitacion") Habitacion habitacion, RedirectAttributes redirectAttributes) {
        String redirectError = habitacion.getId() < 1
            ? "redirect:/habitaciones/nuevo/"
            : "redirect:/habitaciones/editar/" + habitacion.getId() + "/";

        Optional<Habitacion> habitacionExistente = service.consultarPornumerohabitacion(habitacion.getNumero());
        boolean numeroRepetido = habitacionExistente.isPresent() && habitacionExistente.get().getId() != habitacion.getId();
        if (numeroRepetido) {
            redirectAttributes.addFlashAttribute("error", "No se pudo guardar la habitación. N° de habitación repetido.");
            return redirectError;
        }

        boolean esNueva = habitacion.getId() < 1;
        try {
            service.guardar(habitacion);
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                "Esta habitación fue modificada por otra persona mientras la editabas. Recargá la página e intentá de nuevo.");
            return redirectError;
        }
        redirectAttributes.addFlashAttribute("exito", esNueva ? "Habitación creada correctamente." : "Habitación actualizada correctamente.");
        return "redirect:/habitaciones/";
    }

    @PostMapping("/eliminar/{habitacionID}/")
    public String eliminar(
        @PathVariable(name="habitacionID", required = true) int id,
        RedirectAttributes redirectAttributes
    ) {
        boolean tieneRegistros = registroService.listar().stream()
            .anyMatch(r -> r.getHabitacion() != null && r.getHabitacion().getId() == id);
        if (tieneRegistros) {
            redirectAttributes.addFlashAttribute("error",
                "No se puede eliminar: esta habitación tiene registros asociados.");
            return "redirect:/habitaciones/";
        }

        service.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Habitación eliminada correctamente.");
        return "redirect:/habitaciones/";
    }

    // Atajo de un clic para cuando housekeeping termina de limpiar, sin pasar
    // por el formulario completo de edición.
    @PostMapping("/marcar-disponible/{habitacionID}/")
    public String marcarDisponible(
        @PathVariable(name = "habitacionID") int id,
        Model model
    ) {
        service.consultarId(id).ifPresent(habitacion -> {
            habitacion.setEstado(EstadoHabitacion.DISPONIBLE);
            service.guardar(habitacion);
        });
        return listar(model);
    }
    @GetMapping("/disponibles")
    public ResponseEntity<List<Habitacion>> getHabitacionesDisponibles(
            @RequestParam String checkIn,
            @RequestParam int noches) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            Date checkInDate = sdf.parse(checkIn);

            List<Habitacion> habitacionesDisponibles = registroService.obtenerHabitacionesDisponibles(checkInDate, noches);
            return ResponseEntity.ok(habitacionesDisponibles);
        } catch (ParseException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/calcular-tarifa/{habitacionID}")
    public ResponseEntity<Double> calcularTarifa(
    @PathVariable(name="habitacionID", required=true) int id,
    @RequestParam int noches,
    @RequestParam(required = false) String checkIn) {
    Optional<Habitacion> habitacionOptional = service.consultarId(id);

    if (habitacionOptional.isEmpty()) {
        return ResponseEntity.notFound().build();
    }

    Habitacion habitacion = habitacionOptional.get();

    double precioPorNoche = habitacion.getCategoria().getPrecio();
    if (checkIn != null && !checkIn.isBlank()) {
        try {
            Date fechaCheckIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(checkIn);
            precioPorNoche = habitacion.getCategoria().precioParaFecha(fechaCheckIn);
        } catch (ParseException ignored) {
            // Si la fecha no se puede parsear, se usa el precio base.
        }
    }

    double tarifaTotal = precioPorNoche * noches;

    return ResponseEntity.ok(tarifaTotal);
}
}
