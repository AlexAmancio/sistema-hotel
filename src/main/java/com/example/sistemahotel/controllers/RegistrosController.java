package com.example.sistemahotel.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.example.sistemahotel.util.FechaUtils;

@RequestMapping("/")
@Controller
public class RegistrosController {
    String carpeta = "registros/";
    private static final int TAMANO_PAGINA = 15;

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

    @Autowired
    private com.example.sistemahotel.interfaces.IConsumoService consumoService;

    @GetMapping("/")
    public String listar(
        @RequestParam(required = false) String estado,
        @RequestParam(defaultValue = "0") int pagina,
        Model model
    ) {
        EstadoRegistro filtro = parseEstado(estado);
        Page<Registro> paginaRegistros = service.listarPaginado(filtro, PageRequest.of(pagina, TAMANO_PAGINA));
        List<Registro> registros = paginaRegistros.getContent();
        model.addAttribute("registros", registros);
        model.addAttribute("estadoSeleccionado", estado != null ? estado : "");
        model.addAttribute("paginaActual", paginaRegistros.getNumber());
        model.addAttribute("totalPaginas", paginaRegistros.getTotalPages());

        Map<Integer, Comprobante> comprobantesPorRegistro = new HashMap<>();
        for (Registro registro : registros) {
            comprobanteService.consultarPorRegistro(registro.getId())
                .ifPresent(comprobante -> comprobantesPorRegistro.put(registro.getId(), comprobante));
        }
        model.addAttribute("comprobantes", comprobantesPorRegistro);

        return carpeta + "registro-lista";
    }

    private EstadoRegistro parseEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return EstadoRegistro.valueOf(estado);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Usada por el export a CSV, que siempre necesita el listado completo sin paginar.
    private List<Registro> filtrarPorEstado(List<Registro> registros, String estado) {
        EstadoRegistro filtro = parseEstado(estado);
        if (filtro == null) {
            return registros;
        }
        return registros.stream().filter(r -> r.getEstado() == filtro).collect(Collectors.toList());
    }

    @GetMapping("/exportar/")
    public void exportar(@RequestParam(required = false) String estado, HttpServletResponse response) throws IOException {
        List<Registro> registros = filtrarPorEstado(service.listar(), estado);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"registros.csv\"");

        PrintWriter writer = response.getWriter();
        writer.write('﻿'); // BOM para que Excel detecte UTF-8
        writer.println("Cliente,Habitacion,Check-in,Checkout estimado,Estado,Tarifa,Saldo pendiente,Registrado por");
        for (Registro r : registros) {
            writer.println(String.join(",",
                csv(r.getCliente().getNombres() + " " + r.getCliente().getApellidos()),
                csv(r.getHabitacion() != null ? r.getHabitacion().getNumero() : ""),
                csv(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.getCheckIn())),
                csv(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.calcularCheckoutEstimado())),
                csv(r.getEstado().name()),
                csv(String.valueOf(r.getTarifa())),
                csv(String.valueOf(r.getSaldoPendiente())),
                csv(r.getUsuario() != null ? r.getUsuario().getNombres() + " " + r.getUsuario().getApellidos() : "")
            ));
        }
        writer.flush();
    }

    private String csv(String valor) {
        String escapado = valor == null ? "" : valor.replace("\"", "\"\"");
        return "\"" + escapado + "\"";
    }

    @GetMapping("/hoy/")
    public String hoy(Model model) {
        List<Registro> activos = service.listar().stream()
            .filter(r -> r.getEstado() == EstadoRegistro.ACTIVA)
            .collect(Collectors.toList());

        Date hoy = new Date();

        List<Registro> llegan = activos.stream()
            .filter(r -> FechaUtils.mismoDia(r.getCheckIn(), hoy))
            .collect(Collectors.toList());

        List<Registro> salen = activos.stream()
            .filter(r -> FechaUtils.mismoDia(r.calcularCheckoutEstimado(), hoy))
            .collect(Collectors.toList());

        List<Registro> vencidas = activos.stream()
            .filter(r -> r.calcularCheckoutEstimadoAmigable().equals("Vencido"))
            .collect(Collectors.toList());

        model.addAttribute("llegan", llegan);
        model.addAttribute("salen", salen);
        model.addAttribute("vencidas", vencidas);

        return carpeta + "hoy";
    }

    @GetMapping("/nuevo/")
    public String nuevo(
        @RequestParam(required = false) Integer habitacion,
        @RequestParam(required = false) String checkIn,
        Model model
    ) {
        Registro registro = new Registro();
        // Casi siempre hay al menos un huésped: arrancar en 0 obligaba a
        // tocar este campo en cada registro nuevo, incluso cuando ya era 1.
        registro.setNumHuespedes(1);
        // Precarga desde el calendario de ocupacion: clic en un dia libre de
        // una habitacion ya trae la fecha y esa habitacion seleccionadas.
        if (checkIn != null && !checkIn.isBlank()) {
            try {
                registro.setCheckIn(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(checkIn));
            } catch (java.text.ParseException ignored) {
                // Si no se puede parsear, el formulario queda sin fecha precargada.
            }
        }
        model.addAttribute("registro", registro);
        model.addAttribute("listaClientes", clienteService.listar());
        model.addAttribute("listaHabitaciones", habitacionService.listar());
        model.addAttribute("habitacionPreseleccionada", habitacion);
        model.addAttribute("clientesConReservaActiva", clientesConReservaActiva(0));

        model.addAttribute("titulo", "Nuevo registro");
        model.addAttribute("txtboton", "Agregar");

        return carpeta + "registro-form";
    }

    // clienteId -> número de habitación de su reserva ACTIVA, si tiene una.
    // Se usa en el formulario para avisar antes de crearle una segunda
    // reserva activa por error (p. ej. buscarlo de nuevo sin darse cuenta).
    private Map<Integer, String> clientesConReservaActiva(int excluirRegistroId) {
        return service.listar().stream()
            .filter(r -> r.getId() != excluirRegistroId
                && r.getEstado() == EstadoRegistro.ACTIVA
                && r.getCliente() != null
                && r.getHabitacion() != null)
            .collect(Collectors.toMap(
                r -> r.getCliente().getId(),
                r -> r.getHabitacion().getNumero(),
                (primera, segunda) -> primera
            ));
    }

    @PostMapping("/guardar/")
    public String guardar(Registro registro, Model model, RedirectAttributes redirectAttributes) {
        String redirectError = registro.getId() > 0
            ? "redirect:/editar/" + registro.getId() + "/"
            : "redirect:/nuevo/";

        // El formulario ya valida esto en el navegador, pero esa validación
        // puede fallar o saltarse (JS deshabilitado, bug del lado cliente):
        // sin este chequeo acá, un POST sin cliente/habitación igual se
        // guardaba.
        if (registro.getCliente() == null) {
            redirectAttributes.addFlashAttribute("error", "Elegí un cliente antes de guardar.");
            return redirectError;
        }
        if (registro.getHabitacion() == null) {
            redirectAttributes.addFlashAttribute("error", "Elegí una habitación antes de guardar.");
            return redirectError;
        }

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
        boolean esNuevo = registro.getId() == 0;
        if (esNuevo) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            usuarioService.consultarpornombreusuario(username).ifPresent(registro::setUsuario);
        }

        // Si se está editando y se cambió de habitación, hay que liberar la
        // anterior: si no, se quedaba marcada OCUPADA para siempre aunque este
        // registro ya no la use.
        Integer habitacionAnteriorId = null;
        if (!esNuevo) {
            habitacionAnteriorId = service.consultarId(registro.getId())
                .map(r -> r.getHabitacion() != null ? r.getHabitacion().getId() : null)
                .orElse(null);
        }

        try {
            service.guardar(registro);
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                "Este registro fue modificado por otra persona mientras lo editabas. Recargá la página e intentá de nuevo.");
            return redirectError;
        }

        Integer habitacionNuevaId = registro.getHabitacion() != null ? registro.getHabitacion().getId() : null;
        boolean cambioDeHabitacion = habitacionAnteriorId != null && !habitacionAnteriorId.equals(habitacionNuevaId);

        // La habitación se marca OCUPADA en dos casos: un check-in nuevo, o una
        // edición que cambió de habitación mientras el registro sigue ACTIVA.
        // Editar cualquier otra cosa (observaciones, método de pago, etc.) de un
        // registro ya FINALIZADA/CANCELADA no debe tocar el estado de ninguna
        // habitación — antes esto corría en cada guardado sin condición.
        boolean debeOcuparNueva = (esNuevo || cambioDeHabitacion) && registro.getEstado() == com.example.sistemahotel.modelos.EstadoRegistro.ACTIVA;
        if (debeOcuparNueva && registro.getHabitacion() != null) {
            Habitacion habitacion = registro.getHabitacion();
            habitacion.setEstado(EstadoHabitacion.OCUPADA);
            habitacionService.guardar(habitacion);
        }

        if (cambioDeHabitacion) {
            final int habitacionAnteriorIdFinal = habitacionAnteriorId;
            final int registroIdFinal = registro.getId();
            boolean otroRegistroLaUsaActiva = service.listar().stream()
                .anyMatch(r -> r.getId() != registroIdFinal
                    && r.getEstado() == com.example.sistemahotel.modelos.EstadoRegistro.ACTIVA
                    && r.getHabitacion() != null
                    && r.getHabitacion().getId() == habitacionAnteriorIdFinal);
            if (!otroRegistroLaUsaActiva) {
                habitacionService.consultarId(habitacionAnteriorIdFinal).ifPresent(habitacionVieja -> {
                    habitacionVieja.setEstado(EstadoHabitacion.DISPONIBLE);
                    habitacionService.guardar(habitacionVieja);
                });
            }
        }

        return "redirect:/";
    }

    @GetMapping("/editar/{registroID}/")
    public String editar(
        @PathVariable(name="registroID", required=true) int id,
        Model model
    ) {
        // service.consultarId devuelve un Optional: pasarlo directo al modelo
        // rompía cualquier expresión SpEL del template que no fuera un simple
        // th:field (ej. "registro.usuario != null"), porque esas no
        // desenvuelven el Optional automáticamente.
        return service.consultarId(id).map(registro -> {
            model.addAttribute("registro", registro);
            model.addAttribute("listaClientes", clienteService.listar());
            model.addAttribute("listaHabitaciones", habitacionService.listar());
            model.addAttribute("clientesConReservaActiva", clientesConReservaActiva(id));
            model.addAttribute("titulo", "Editar registro");
            model.addAttribute("txtboton", "Actualizar");
            return carpeta + "registro-form";
        }).orElse("redirect:/");
    }


    @PostMapping("/eliminar/{registroID}/")
    public String eliminar(
        @PathVariable(name="registroID", required = true) int id,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (comprobanteService.consultarPorRegistro(id).isPresent()) {
            redirectAttributes.addFlashAttribute("error",
                "No se puede eliminar: este registro ya tiene un comprobante emitido.");
            return "redirect:/";
        }
        if (!consumoService.listarPorRegistro(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                "No se puede eliminar: este registro tiene consumos cargados.");
            return "redirect:/";
        }
        service.eliminar(id);
        return "redirect:/";
    }

    @PostMapping("/checkout/{registroID}/")
    public String checkout(@PathVariable(name="registroID") int id, RedirectAttributes redirectAttributes) {
        try {
            service.hacerCheckout(id);
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                "Este registro fue modificado por otra persona justo ahora. Recargá la página e intentá de nuevo.");
        }
        return "redirect:/";
    }

    @PostMapping("/cancelar/{registroID}/")
    public String cancelar(@PathVariable(name="registroID") int id, RedirectAttributes redirectAttributes) {
        try {
            service.cancelar(id);
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                "Este registro fue modificado por otra persona justo ahora. Recargá la página e intentá de nuevo.");
        }
        return "redirect:/";
    }

    @PostMapping("/no-show/{registroID}/")
    public String noShow(@PathVariable(name="registroID") int id, RedirectAttributes redirectAttributes) {
        try {
            service.marcarNoShow(id);
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("error",
                "Este registro fue modificado por otra persona justo ahora. Recargá la página e intentá de nuevo.");
        }
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
