package com.example.sistemahotel.repositories;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.Cliente;

@Repository
public interface IClienteRepository extends JpaRepository<Cliente, Integer>{
    Optional<Cliente> findByIdentificacion(String identificacion);
}
