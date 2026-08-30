package com.example.sistemahotel.interfaces;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

public interface IRegistroService {
    public List<Registro> listar();
    public Page<Registro> listarPaginado(EstadoRegistro estado, Pageable pageable);
    public Optional<Registro> consultarId(int id);
    public void guardar(Registro registro);
    public void eliminar(int id);
    List<Habitacion> obtenerHabitacionesDisponibles(Date checkIn, int noches);
    boolean existeSolapamiento(Habitacion habitacion, Date checkIn, int noches, Integer excluirRegistroId);
    void hacerCheckout(int registroId);
    void cancelar(int registroId);
    void marcarNoShow(int registroId);
    // Devuelve false si la extensión chocaría con otra reserva activa.
    boolean extenderEstadia(int registroId, int nuevasNoches);
}
