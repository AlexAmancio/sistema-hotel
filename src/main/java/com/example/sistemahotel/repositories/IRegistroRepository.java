package com.example.sistemahotel.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Registro;

@Repository
public interface IRegistroRepository extends JpaRepository<Registro, Integer>{
    Page<Registro> findByEstado(EstadoRegistro estado, Pageable pageable);
}
