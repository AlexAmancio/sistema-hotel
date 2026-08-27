package com.example.sistemahotel.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.Consumo;

@Repository
public interface IConsumoRepository extends CrudRepository<Consumo, Integer> {
    List<Consumo> findByRegistroId(int registroId);
}
