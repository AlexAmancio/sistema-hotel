package com.example.sistemahotel.repositories;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.Categoria;

@Repository
public interface ICategoriaRepository extends CrudRepository<Categoria, Integer>{
    Optional<Categoria> findByNombre(String Nombre);
}
