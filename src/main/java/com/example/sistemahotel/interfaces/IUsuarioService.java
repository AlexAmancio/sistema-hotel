package com.example.sistemahotel.interfaces;

import java.util.List;
import java.util.Optional;

import com.example.sistemahotel.modelos.Usuario;

public interface IUsuarioService {
    public List<Usuario> listar();
    public Optional<Usuario> consultarId(int id);
    public void guardar(Usuario usuario);
    public void eliminar(int id);
    public Optional <Usuario> consultarpornombreusuario(String username);
}
