package com.example.sistemahotel.interfaces;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.modelos.Reserva;
import com.example.sistemahotel.modelos.Usuario;

public interface IReservaService {
    Optional<Reserva> consultarId(int id);

    // Lanza IllegalArgumentException con un mensaje entendible si alguna
    // habitación no tiene aforo suficiente o ya tiene otra reserva activa
    // que se cruza con esas fechas. No crea nada a medias: o se crean todas
    // las habitaciones de la reserva, o ninguna.
    // huespedesPorHabitacion mapea habitacionId -> cantidad de huéspedes en
    // esa habitación puntual (cada habitación de la reserva puede llevar un
    // número distinto de huéspedes).
    Reserva crear(Cliente cliente, Usuario usuario, Date checkIn, int noches, Map<Integer, Integer> huespedesPorHabitacion, List<Integer> habitacionIds);
}
