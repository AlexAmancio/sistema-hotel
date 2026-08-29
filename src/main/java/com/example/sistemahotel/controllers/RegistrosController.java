package com.example.sistemahotel.controllers;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.interfaces.IComprobanteService;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

@RequestMapping("/")
@Controller
public class RegistrosController {
    String carpeta = "registros/";

    @Autowired
    private IRegistroService service;

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private IHabitacionService habitacionService;

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private IComprobanteService comprobanteService;

    @GetMapping("/")
    public String listar(Model model) {
        List<Registro> registros = service.listar();
        model.addAttribute("registros", registros);

        Map<Integer, Comprobante> comprobantesPorRegistro = new HashMap<>();
        for (Registro registro : registros) {
            comprobanteService.consultarPorRegistro(registro.getId())
                .ifPresent(comprobante -> comprobantesPorRegistro.put(registro.getId(), comprobante));
        }
        model.addAttribute("comprobantes", comprobantesPorRegistro);

        return carpeta + "registro-lista";
    }

    @GetMapping("/hoy/")
    public String hoy(Model model) {
        List<Registro> activos = service.listar().stream()
            .filter(r -> r.getEstado() == EstadoRegistro.ACTIVA)
            .collect(Collectors.toList());

        Date hoy = new Date();

        List<Registro> llegan = activos.stream()
            .filter(r -> esMismoDia(r.getCheckIn(), hoy))
            .collect(Collectors.toList());

        List<Registro> salen = activos.stream()
            .filter(r -> esMismoDia(r.calcularCheckoutEstimado(), hoy))
            .collect(Collectors.toList());

        List<Registro> vencidas = activos.stream()
            .filter(r -> r.calcularCheckoutEstimadoAmigable().equals("Vencido"))
            .collect(Collectors.toList());

        model.addAttribute("llegan", llegan);
        model.addAttribute("salen", salen);
        model.addAttribute("vencidas", vencidas);

        return carpeta + "hoy";
    }

    private boolean esMismoDia(Date d1, Date d2) {
        if (d1 == null || d2 == null) {
            return false;
        }
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
            && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    @GetMapping("/nuevo/")
    public String nuevo(Model model) {
        model.addAttribute("registro", new Registro());
        model.addAttribute("listaClientes", clienteService.listar());
        model.addAttribute("listaHabitaciones", habitacionService.listar());

        model.addAttribute("titulo", "Nuevo registro");
        model.addAttribute("txtboton", "Agregar");

        return carpeta + "registro-form";
    }

    @PostMapping("/guardar/")
    public String guardar(Registro registro, Model model, RedirectAttributes redirectAttributes) {
        String redirectError = registro.getId() > 0
            ? "redirect:/editar/" + registro.getId() + "/"
            : "redirect:/nuevo/";

        if (registro.getHabitacion() != null && registro.getNumHuespedes() > registro.getHabitacion().getAforo()) {
            redirectAttributes.addFlashAttribute("error",
                "El número de huéspedes (" + registro.getNumHuespedes() + ") supera el aforo máximo de la habitación " +
                registro.getHabitacion().getNumero() + " (" + registro.getHabitacion().getAforo() + " personas).");
            return redirectError;
        }

        if (registro.getHabitacion() != null && registro.getCheckIn() != null) {
            Integer idActual = registro.getId() > 0 ? registro.getId() : null;
            boolean solapa = service.existeSolapamiento(registro.getHabitacion(), registro.getCheckIn(), registro.getNoches(), idActual);
            if (solapa) {
                redirectAttributes.addFlashAttribute("error",
                    "La habitación " + registro.getHabitacion().getNumero() + " ya tiene otra reserva activa que se cruza con esas fechas.");
                return redirectError;
            }
        }

        // Un check-in nuevo queda atribuido al usuario logueado, no al que
        // el form haya podido mandar (evita que se falsee quién lo registró).
        if (registro.getId() == 0) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            usuarioService.consultarpornombreusuario(username).ifPresent(registro::setUsuario);
        }

        try {
            service.guardar(registro);
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                "Este registro fue modificado por otra persona mientras lo editabas. Recargá la página e intentá de nuevo.");
            return redirectError;
        }

        // Un check-in nuevo ocupa la habitación automáticamente.
        if (registro.getHabitacion() != null) {
            Habitacion habitacion = registro.getHabitacion();
            habitacion.setEstado(EstadoHabitacion.OCUPADA);
            habitacionService.guardar(habitacion);
        }

        return "redirect:/";
    }

    @GetMapping("/editar/{registroID}/")
    public String editar(
        @PathVariable(name="registroID", required=true) int id,
        Model model
    ) {
        model.addAttribute("registro", service.consultarId(id));
        model.addAttribute("listaClientes", clienteService.listar());
        model.addAttribute("listaHabitaciones", habitacionService.listar());

        model.addAttribute("titulo", "Editar registro");
        model.addAttribute("txtboton", "Actualizar");

        return carpeta + "registro-form";
    }


    @PostMapping("/eliminar/{registroID}/")
    public String eliminar(
        @PathVariable(name="registroID", required = true) int id,
        Model model
    ) {
        service.eliminar(id);
        return "redirect:/";
    }

    @PostMapping("/checkout/{registroID}/")
    public String checkout(@PathVariable(name="registroID") int id) {
        service.hacerCheckout(id);
        return "redirect:/";
    }

    @PostMapping("/cancelar/{registroID}/")
    public String cancelar(@PathVariable(name="registroID") int id) {
        service.cancelar(id);
        return "redirect:/";
    }

    @PostMapping("/no-show/{registroID}/")
    public String noShow(@PathVariable(name="registroID") int id) {
        service.marcarNoShow(id);
        return "redirect:/";
    }

    @GetMapping("/extender/{registroID}/")
    public String extenderForm(@PathVariable(name="registroID") int id, Model model) {
        return service.consultarId(id).map(registro -> {
            model.addAttribute("registro", registro);
            return carpeta + "extender-form";
        }).orElse("redirect:/");
    }

    @PostMapping("/extender/{registroID}/")
    public String extender(
        @PathVariable(name="registroID") int id,
        @RequestParam int noches,
        RedirectAttributes redirectAttributes
    ) {
        boolean ok = service.extenderEstadia(id, noches);
        if (!ok) {
            redirectAttributes.addFlashAttribute("error",
                "No se pudo extender: la habitación ya tiene otra reserva activa que se cruzaría con las nuevas fechas.");
            return "redirect:/extender/" + id + "/";
        }
        return "redirect:/";
    }

}
