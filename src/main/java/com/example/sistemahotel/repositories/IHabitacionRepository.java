package com.example.sistemahotel.repositories;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.Habitacion;

@Repository
public interface IHabitacionRepository extends CrudRepository<Habitacion, Integer> {
    Optional<Habitacion> findByNumero(String numero);
}
