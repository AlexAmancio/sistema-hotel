package com.example.sistemahotel.interfaces;

import java.util.List;
import java.util.Optional;

import com.example.sistemahotel.modelos.Habitacion;

public interface IHabitacionService {
    public List<Habitacion> listar();
    public Optional<Habitacion> consultarId(int id);
    public void guardar(Habitacion habitacion);
    public void eliminar(int id);
    public Optional<Habitacion> consultarPornumerohabitacion(String numero);
}
