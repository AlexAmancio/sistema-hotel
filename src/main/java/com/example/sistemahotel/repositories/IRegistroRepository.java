package com.example.sistemahotel.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import com.example.sistemahotel.modelos.Registro;

@Repository
public interface IRegistroRepository extends CrudRepository<Registro, Integer>{
    
}
