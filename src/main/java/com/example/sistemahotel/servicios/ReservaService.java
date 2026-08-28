package com.example.sistemahotel.servicios;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.interfaces.IReservaService;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.modelos.Reserva;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IReservaRepository;

@Service
public class ReservaService implements IReservaService {

    @Autowired
    private IReservaRepository repositorio;

    @Autowired
    private IHabitacionService habitacionService;

    @Autowired
    private IRegistroService registroService;

    @Override
    public Optional<Reserva> consultarId(int id) {
        return repositorio.findById(id);
    }

    @Override
    @Transactional
    public Reserva crear(Cliente cliente, Usuario usuario, Date checkIn, int noches, Map<Integer, Integer> huespedesPorHabitacion, List<Integer> habitacionIds) {
        if (habitacionIds == null || habitacionIds.isEmpty()) {
            throw new IllegalArgumentException("Elegí al menos una habitación para la reserva.");
        }

        // Se valida TODO primero (aforo y solapamiento de cada habitación) antes
        // de crear nada: o entra la reserva completa, o no se crea ninguna parte.
        List<Habitacion> habitaciones = new ArrayList<>();
        for (Integer habitacionId : habitacionIds) {
            Habitacion habitacion = habitacionService.consultarId(habitacionId)
                .orElseThrow(() -> new IllegalArgumentException("Una de las habitaciones seleccionadas ya no existe."));

            int huespedes = huespedesPorHabitacion.getOrDefault(habitacionId, 1);
            if (huespedes > habitacion.getAforo()) {
                throw new IllegalArgumentException(
                    "La habitación " + habitacion.getNumero() + " tiene aforo para " + habitacion.getAforo() +
                    " persona(s), y pediste " + huespedes + ".");
            }

            if (registroService.existeSolapamiento(habitacion, checkIn, noches, null)) {
                throw new IllegalArgumentException(
                    "La habitación " + habitacion.getNumero() + " ya tiene otra reserva activa que se cruza con esas fechas.");
            }

            habitaciones.add(habitacion);
        }

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setUsuario(usuario);
        reserva = repositorio.save(reserva);

        for (Habitacion habitacion : habitaciones) {
            double precioPorNoche = habitacion.getCategoria().precioParaFecha(checkIn);

            Registro registro = new Registro();
            registro.setCliente(cliente);
            registro.setUsuario(usuario);
            registro.setHabitacion(habitacion);
            registro.setCheckIn(checkIn);
            registro.setNoches(noches);
            registro.setNumHuespedes(huespedesPorHabitacion.getOrDefault(habitacion.getId(), 1));
            registro.setTarifa(precioPorNoche * noches);
            registro.setReserva(reserva);
            registroService.guardar(registro);

            habitacion.setEstado(EstadoHabitacion.OCUPADA);
            habitacionService.guardar(habitacion);
        }

        return reserva;
    }
}
