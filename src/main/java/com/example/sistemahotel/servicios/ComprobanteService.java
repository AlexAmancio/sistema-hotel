package com.example.sistemahotel.servicios;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.sistemahotel.interfaces.IComprobanteService;
import com.example.sistemahotel.modelos.Comprobante;
import com.example.sistemahotel.modelos.Consumo;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IComprobanteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

// Genera un comprobante "local": tiene la forma de un comprobante electrónico
// peruano (serie-correlativo, IGV desglosado, snapshot en JSON) pero no está
// firmado ni fue enviado a SUNAT. El JSON está pensado para poder alimentar
// más adelante la API de un OSE homologado (ej. Nubefact) sin rediseñar nada.
@Service
public class ComprobanteService implements IComprobanteService {

    private static final String SERIE = "C001";
    private static final double TASA_IGV = 0.18;

    @Autowired
    private IComprobanteRepository repositorio;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public Optional<Comprobante> consultarPorRegistro(int registroId) {
        return repositorio.findByRegistroId(registroId);
    }

    @Override
    public Comprobante emitir(Registro registro) {
        Optional<Comprobante> existente = repositorio.findByRegistroId(registro.getId());
        if (existente.isPresent()) {
            return existente.get();
        }

        long correlativo = repositorio.countBySerie(SERIE) + 1;

        double total = registro.getTotalEstadia();
        double valorVenta = total / (1 + TASA_IGV);
        double igv = total - valorVenta;

        Comprobante comprobante = new Comprobante();
        comprobante.setSerie(SERIE);
        comprobante.setCorrelativo((int) correlativo);
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setValorVenta(redondear(valorVenta));
        comprobante.setIgv(redondear(igv));
        comprobante.setTotal(redondear(total));
        comprobante.setRegistro(registro);
        comprobante.setDatosJson(construirJson(comprobante, registro));

        return repositorio.save(comprobante);
    }

    private String construirJson(Comprobante comprobante, Registro registro) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("serie", comprobante.getSerie());
        data.put("numero", String.format("%08d", comprobante.getCorrelativo()));
        data.put("fecha_de_emision", comprobante.getFechaEmision().toLocalDate().toString());
        data.put("moneda", "PEN");

        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("tipo_de_documento", "DNI");
        cliente.put("numero_de_documento", registro.getCliente().getIdentificacion());
        cliente.put("denominacion", registro.getCliente().getNombres() + " " + registro.getCliente().getApellidos());
        data.put("cliente", cliente);

        List<Map<String, Object>> items = new ArrayList<>();

        Map<String, Object> itemEstadia = new LinkedHashMap<>();
        itemEstadia.put("descripcion", "Estadía - Habitación " + registro.getHabitacion().getNumero()
            + " (" + registro.getHabitacion().getCategoria().getNombre() + ") - "
            + registro.getNoches() + " noche(s)");
        itemEstadia.put("cantidad", 1);
        itemEstadia.put("total", registro.getTarifa() != null ? registro.getTarifa() : 0.0);
        items.add(itemEstadia);

        if (registro.getConsumos() != null) {
            for (Consumo consumo : registro.getConsumos()) {
                Map<String, Object> itemConsumo = new LinkedHashMap<>();
                itemConsumo.put("descripcion", consumo.getDescripcion());
                itemConsumo.put("cantidad", 1);
                itemConsumo.put("total", consumo.getMonto());
                items.add(itemConsumo);
            }
        }
        data.put("items", items);

        data.put("valor_venta", comprobante.getValorVenta());
        data.put("total_igv", comprobante.getIgv());
        data.put("total", comprobante.getTotal());

        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
