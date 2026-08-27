package com.example.sistemahotel.repositories;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.Usuario;

@Repository
public interface IUsuarioRepository extends CrudRepository<Usuario, Integer> {
    public Optional<Usuario> findByUsername(String username);
}
