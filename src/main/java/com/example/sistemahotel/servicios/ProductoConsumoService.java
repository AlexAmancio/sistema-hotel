package com.example.sistemahotel.servicios;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.sistemahotel.interfaces.IProductoConsumoService;
import com.example.sistemahotel.modelos.ProductoConsumo;
import com.example.sistemahotel.repositories.IProductoConsumoRepository;

@Service
public class ProductoConsumoService implements IProductoConsumoService {

    @Autowired
    private IProductoConsumoRepository repositorio;

    @Override
    public List<ProductoConsumo> listar() {
        return repositorio.findAllByOrderByNombreAsc();
    }

    @Override
    public List<ProductoConsumo> listarActivos() {
        return repositorio.findByActivoTrueOrderByNombreAsc();
    }

    @Override
    public Optional<ProductoConsumo> consultarId(int id) {
        return repositorio.findById(id);
    }

    @Override
    public void guardar(ProductoConsumo producto) {
        repositorio.save(producto);
    }

    @Override
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }
}
