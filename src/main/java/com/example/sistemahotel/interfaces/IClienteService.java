package com.example.sistemahotel.interfaces;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.sistemahotel.modelos.Cliente;

public interface IClienteService {
    public List<Cliente> listar();
    public Page<Cliente> listarPaginado(Pageable pageable);
    public Optional<Cliente> consultarId(int id);
    public void guardar(Cliente cliente);
    public void eliminar(int id);
    public  Optional<Cliente> consultarPorIdentificacion(String identificacion);
}
