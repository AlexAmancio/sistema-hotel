package com.example.sistemahotel.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.Reserva;

@Repository
public interface IReservaRepository extends CrudRepository<Reserva, Integer> {
}
