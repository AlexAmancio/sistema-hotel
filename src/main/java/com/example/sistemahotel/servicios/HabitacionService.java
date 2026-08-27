package com.example.sistemahotel.servicios;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.sistemahotel.interfaces.IHabitacionService;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.repositories.IHabitacionRepository;

@Service
public class HabitacionService implements IHabitacionService {

    @Autowired
    private IHabitacionRepository repositorio;

    @Override
    public List<Habitacion> listar() {
        return (List<Habitacion>)repositorio.findAll();
    }

    @Override
    public Optional<Habitacion> consultarId(int id) {
        return repositorio.findById(id);
    }

    @Override
    public void guardar(Habitacion habitacion) {
        repositorio.save(habitacion);
    }

    @Override
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }
    @Override
    public Optional<Habitacion> consultarPornumerohabitacion(String numero) {
        return repositorio.findByNumero(numero);
    }
    
}
