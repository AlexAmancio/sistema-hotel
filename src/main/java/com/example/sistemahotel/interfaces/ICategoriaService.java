package com.example.sistemahotel.interfaces;

import java.util.List;
import java.util.Optional;

import com.example.sistemahotel.modelos.Categoria;

public interface ICategoriaService {
    public List<Categoria> listar();
    public Optional<Categoria> consultarId(int id);
    public void guardar(Categoria categoria);
    public void eliminar(int id);
    public Optional<Categoria> consultarPorNombrecategoria(String Nombre);
}
