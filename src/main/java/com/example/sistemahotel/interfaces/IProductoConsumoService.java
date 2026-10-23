package com.example.sistemahotel.interfaces;

import java.util.List;
import java.util.Optional;

import com.example.sistemahotel.modelos.ProductoConsumo;

public interface IProductoConsumoService {
    List<ProductoConsumo> listar();
    List<ProductoConsumo> listarActivos();
    Optional<ProductoConsumo> consultarId(int id);
    void guardar(ProductoConsumo producto);
    void eliminar(int id);
}
