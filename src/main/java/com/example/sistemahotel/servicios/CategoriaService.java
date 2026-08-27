package com.example.sistemahotel.servicios;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.sistemahotel.interfaces.ICategoriaService;
import com.example.sistemahotel.modelos.Categoria;
import com.example.sistemahotel.repositories.ICategoriaRepository;

@Service
public class CategoriaService implements ICategoriaService {

    @Autowired
    ICategoriaRepository repositorio;

    @Override
    public List<Categoria> listar() {
        return (List<Categoria>)repositorio.findAll();
    }

    @Override
    public Optional<Categoria> consultarId(int id) {
        return repositorio.findById(id);
    }

    @Override
    public void guardar(Categoria categoria) {
        repositorio.save(categoria);
    }

    @Override
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }
    @Override
    public Optional<Categoria> consultarPorNombrecategoria(String Nombre) {
        return repositorio.findByNombre(Nombre);
    }
}
