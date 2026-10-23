package com.example.sistemahotel.interfaces;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;

public interface IRegistroService {
    public List<Registro> listar();
    // Orden operativo para el panel de recepción: vencidas primero (la que
    // lleva más tiempo vencida, primero), luego activas por checkout más
    // próximo, y al final el historial (finalizada/cancelada/no-show) con lo
    // más reciente arriba.
    public List<Registro> listarOrdenadoPorPrioridad(EstadoRegistro estado);
    public Optional<Registro> consultarId(int id);
    public void guardar(Registro registro);
    public void eliminar(int id);
    List<Habitacion> obtenerHabitacionesDisponibles(Date checkIn, int noches, Integer excluirRegistroId);
    boolean existeSolapamiento(Habitacion habitacion, Date checkIn, int noches, Integer excluirRegistroId);
    void hacerCheckout(int registroId);
    void cancelar(int registroId);
    void marcarNoShow(int registroId);
    // Devuelve false si la extensión chocaría con otra reserva activa.
    boolean extenderEstadia(int registroId, int nuevasNoches);
    List<Registro> buscarPorNumeroHabitacion(String query);
}
