package com.example.sistemahotel.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.example.sistemahotel.modelos.ProductoConsumo;

@Repository
public interface IProductoConsumoRepository extends CrudRepository<ProductoConsumo, Integer> {
    List<ProductoConsumo> findByActivoTrueOrderByNombreAsc();
    List<ProductoConsumo> findAllByOrderByNombreAsc();
}
