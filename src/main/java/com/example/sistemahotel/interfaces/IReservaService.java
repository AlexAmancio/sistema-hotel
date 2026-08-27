package com.example.sistemahotel.interfaces;

import java.util.Date;
import java.util.List;
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
    Reserva crear(Cliente cliente, Usuario usuario, Date checkIn, int noches, int huespedesPorHabitacion, List<Integer> habitacionIds);
}
