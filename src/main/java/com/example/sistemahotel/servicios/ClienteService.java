package com.example.sistemahotel.servicios;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.example.sistemahotel.interfaces.IClienteService;
import com.example.sistemahotel.modelos.Cliente;
import com.example.sistemahotel.repositories.IClienteRepository;

@Service
public class ClienteService implements IClienteService{

    @Autowired
    IClienteRepository repositorio;

    @Override
    public List<Cliente> listar() {
        return (List<Cliente>)repositorio.findAll();
    }

    @Override
    public Page<Cliente> listarPaginado(Pageable pageable) {
        return repositorio.findAll(pageable);
    }

    @Override
    public Optional<Cliente> consultarId(int id) {
        return repositorio.findById(id);
    }

    @Override
    public void guardar(Cliente cliente) {
        repositorio.save(cliente);
    }

    @Override
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }
    @Override
    public Optional<Cliente> consultarPorIdentificacion(String identificacion) {
        return repositorio.findByIdentificacion(identificacion);
    }
    
}
