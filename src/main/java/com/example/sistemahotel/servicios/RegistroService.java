package com.example.sistemahotel.servicios;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.example.sistemahotel.interfaces.IRegistroService;
import com.example.sistemahotel.modelos.EstadoHabitacion;
import com.example.sistemahotel.modelos.EstadoRegistro;
import com.example.sistemahotel.modelos.Habitacion;
import com.example.sistemahotel.modelos.Registro;
import com.example.sistemahotel.repositories.IHabitacionRepository;
import com.example.sistemahotel.repositories.IRegistroRepository;

@Service
public class RegistroService implements IRegistroService {

    @Autowired
    private IRegistroRepository repositorio;
    @Autowired
    private IHabitacionRepository habitacionrepositorio;

    @Override
    public List<Registro> listar() {
        return (List<Registro>) repositorio.findAll();
    }

    @Override
    public Page<Registro> listarPaginado(EstadoRegistro estado, Pageable pageable) {
        return estado == null ? repositorio.findAll(pageable) : repositorio.findByEstado(estado, pageable);
    }

    @Override
    public Optional<Registro> consultarId(int id) {
        return repositorio.findById(id);
    }

    @Override
    public void guardar(Registro registro) {
        repositorio.save(registro);
    }

    @Override
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }

    public List<Habitacion> obtenerHabitacionesDisponibles(Date checkIn, int noches) {
        List<Habitacion> todasHabitaciones = (List<Habitacion>) habitacionrepositorio.findAll();
        List<Registro> registrosActivos = registrosActivos();

        Date checkOutEstimado = sumarDias(checkIn, noches);

        List<Habitacion> habitacionesOcupadas = registrosActivos.stream()
            .filter(registro -> seSuperponen(checkIn, checkOutEstimado, registro))
            .map(Registro::getHabitacion)
            .collect(Collectors.toList());

        return todasHabitaciones.stream()
            .filter(habitacion -> habitacion.getEstado() != EstadoHabitacion.MANTENIMIENTO
                                && habitacion.getEstado() != EstadoHabitacion.LIMPIEZA)
            .filter(habitacion -> !habitacionesOcupadas.contains(habitacion))
            .collect(Collectors.toList());
    }

    @Override
    public boolean existeSolapamiento(Habitacion habitacion, Date checkIn, int noches, Integer excluirRegistroId) {
        Date checkOutEstimado = sumarDias(checkIn, noches);

        return registrosActivos().stream()
            .filter(registro -> excluirRegistroId == null || registro.getId() != excluirRegistroId)
            .filter(registro -> registro.getHabitacion() != null && registro.getHabitacion().getId() == habitacion.getId())
            .anyMatch(registro -> seSuperponen(checkIn, checkOutEstimado, registro));
    }

    @Override
    public void hacerCheckout(int registroId) {
        repositorio.findById(registroId).ifPresent(registro -> {
            registro.setCheckOutReal(new Date());
            registro.setEstado(EstadoRegistro.FINALIZADA);
            repositorio.save(registro);

            Habitacion habitacion = registro.getHabitacion();
            if (habitacion != null) {
                habitacion.setEstado(EstadoHabitacion.LIMPIEZA);
                habitacionrepositorio.save(habitacion);
            }
        });
    }

    @Override
    public void cancelar(int registroId) {
        // Si el huésped ya había entrado (la habitación estaba Ocupada), se
        // trata igual que un check-out: pasa por limpieza antes de ofrecerse
        // de nuevo. Si todavía no había llegado, no hace falta limpiarla.
        cambiarEstado(registroId, EstadoRegistro.CANCELADA, true);
    }

    @Override
    public void marcarNoShow(int registroId) {
        // El huésped nunca llegó a usar la habitación, así que no necesita limpieza.
        cambiarEstado(registroId, EstadoRegistro.NO_SHOW, false);
    }

    @Override
    public boolean extenderEstadia(int registroId, int nuevasNoches) {
        Optional<Registro> registroOpt = repositorio.findById(registroId);
        if (registroOpt.isEmpty()) {
            return false;
        }
        Registro registro = registroOpt.get();
        if (registro.getHabitacion() == null) {
            return false;
        }
        boolean solapa = existeSolapamiento(registro.getHabitacion(), registro.getCheckIn(), nuevasNoches, registroId);
        if (solapa) {
            return false;
        }
        registro.setNoches(nuevasNoches);
        repositorio.save(registro);
        return true;
    }

    private void cambiarEstado(int registroId, EstadoRegistro estado, boolean pasarPorLimpiezaSiOcupada) {
        repositorio.findById(registroId).ifPresent(registro -> {
            registro.setEstado(estado);
            repositorio.save(registro);

            Habitacion habitacion = registro.getHabitacion();
            if (habitacion != null && habitacion.getEstado() == EstadoHabitacion.OCUPADA) {
                habitacion.setEstado(pasarPorLimpiezaSiOcupada ? EstadoHabitacion.LIMPIEZA : EstadoHabitacion.DISPONIBLE);
                habitacionrepositorio.save(habitacion);
            }
        });
    }

    // Solo las reservas ACTIVA bloquean disponibilidad: una cancelada, un
    // no-show o una ya finalizada (check-out real hecho) no ocupan la habitación.
    private List<Registro> registrosActivos() {
        return ((List<Registro>) repositorio.findAll()).stream()
            .filter(registro -> registro.getEstado() == EstadoRegistro.ACTIVA)
            .collect(Collectors.toList());
    }

    private boolean seSuperponen(Date rangoInicio, Date rangoFin, Registro registro) {
        Date registroCheckIn = registro.getCheckIn();
        Date registroCheckOut = sumarDias(registroCheckIn, registro.getNoches());
        return rangoInicio.before(registroCheckOut) && rangoFin.after(registroCheckIn);
    }

    private Date sumarDias(Date fecha, int dias) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(fecha);
        calendar.add(Calendar.DAY_OF_MONTH, dias);
        return calendar.getTime();
    }

}
