package com.example.sistemahotel.controllers;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.util.TextoUtils;

@RequestMapping("/clientes/")
@Controller
public class ClientesController {
    String carpeta = "clientes/";

    @Autowired
    private IClienteService service;

    @Value("${apiperu.token}")
    private String apiPeruToken;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/")
    public String listar(Model model) {
        model.addAttribute("listaClientes", service.listar());

        return carpeta + "cliente-lista";
    }

    @GetMapping("/nuevo/")
    public String nuevo(Model model) {
        model.addAttribute("cliente", new Cliente());
        model.addAttribute("titulo", "Agregar cliente");
        model.addAttribute("txtboton", "Agregar");
        return carpeta + "cliente-form";
    }

    @GetMapping("/editar/{idCliente}/")
    public String editar(
        @PathVariable(name="idCliente", required=true) int id,
        Model model
    ) {
        model.addAttribute("cliente", service.consultarId(id));
        model.addAttribute("titulo", "Editar cliente");
        model.addAttribute("txtboton", "Actualizar");
        return carpeta + "cliente-form";
    }

    @PostMapping("/guardar/")
    public String guardar(
        @ModelAttribute("cliente") Cliente cliente,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        cliente.setFechaNacimiento(new Date());

        // Nombres, apellidos, ciudad y país se guardan con la primera letra de
        // cada palabra en mayúscula, sin importar cómo los haya tipeado el
        // recepcionista (o cómo vengan de la API de DNI, que devuelve todo en
        // mayúsculas). El correo NO se toca.
        cliente.setNombres(TextoUtils.capitalizarNombre(cliente.getNombres()));
        cliente.setApellidos(TextoUtils.capitalizarNombre(cliente.getApellidos()));
        cliente.setCiudad(TextoUtils.capitalizarNombre(cliente.getCiudad()));
        cliente.setPais(TextoUtils.capitalizarNombre(cliente.getPais()));

        if(cliente.getId()<1){
            Optional<Cliente> clienteExistente = service.consultarPorIdentificacion(cliente.getIdentificacion());
            if (clienteExistente.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "No se pudo registrar el cliente. DNI repetido.");
                return "redirect:/clientes/nuevo/";
            }
            service.guardar(cliente);
            return listar(model);
        }
        service.guardar(cliente);
        return listar(model);
    }

    @PostMapping("/eliminar/{idCliente}/")
    public String eliminar(
        @PathVariable(name="idCliente", required = true) int id,
        Model model
    ) {
        service.eliminar(id);
        return listar(model);
    }

    // Proxy server-side a apiperu.dev: el token nunca viaja al navegador.
    @GetMapping("/dni/{dni}")
    public ResponseEntity<String> buscarPorDni(@PathVariable String dni) {
        if (!dni.matches("\\d{8}")) {
            return ResponseEntity.badRequest().body("{\"error\":\"DNI inválido\"}");
        }
        String url = "https://apiperu.dev/api/dni/" + dni + "?api_token=" + apiPeruToken;
        try {
            return ResponseEntity.ok(restTemplate.getForObject(url, String.class));
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"No se pudo consultar el DNI\"}");
        }
    }

}
