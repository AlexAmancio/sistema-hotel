package com.example.sistemahotel.repositories;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.Comprobante;

@Repository
public interface IComprobanteRepository extends CrudRepository<Comprobante, Integer> {
    Optional<Comprobante> findByRegistroId(int registroId);
    long countBySerie(String serie);
}
