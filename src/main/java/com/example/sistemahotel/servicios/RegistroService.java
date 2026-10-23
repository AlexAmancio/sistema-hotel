package com.example.sistemahotel.servicios;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
    public List<Registro> listarOrdenadoPorPrioridad(EstadoRegistro estado) {
        List<Registro> registros = listar().stream()
            .filter(registro -> estado == null || registro.getEstado() == estado)
            .collect(Collectors.toList());
        registros.sort(COMPARADOR_PRIORIDAD_OPERATIVA);
        return registros;
    }

    // 0 = vencida (activa, ya pasó el checkout estimado), 1 = activa al día,
    // 2 = historial (finalizada/cancelada/no-show).
    private static int prioridadOperativa(Registro registro) {
        if (registro.getEstado() != EstadoRegistro.ACTIVA) {
            return 2;
        }
        return "Vencido".equals(registro.calcularCheckoutEstimadoAmigable()) ? 0 : 1;
    }

    private static final Comparator<Registro> COMPARADOR_PRIORIDAD_OPERATIVA = (a, b) -> {
        int prioridadA = prioridadOperativa(a);
        int prioridadB = prioridadOperativa(b);
        if (prioridadA != prioridadB) {
            return Integer.compare(prioridadA, prioridadB);
        }
        if (prioridadA != 2) {
            // Activas (vencidas o al día): la que se va primero, primero.
            return a.calcularCheckoutEstimado().compareTo(b.calcularCheckoutEstimado());
        }
        // Historial: lo más reciente arriba.
        return b.getCheckIn().compareTo(a.getCheckIn());
    };

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
        repositorio.findById(id).ifPresent(registro -> {
            // Si este registro era el que tenía la habitación Ocupada, hay que
            // liberarla antes de borrarlo: si no, queda marcada Ocupada para
            // siempre sin ningún registro detrás que la explique, y nadie
            // puede volver a usarla ni hacerle check-out.
            Habitacion habitacion = registro.getHabitacion();
            if (registro.getEstado() == EstadoRegistro.ACTIVA
                    && habitacion != null && habitacion.getEstado() == EstadoHabitacion.OCUPADA) {
                habitacion.setEstado(EstadoHabitacion.DISPONIBLE);
                habitacionrepositorio.save(habitacion);
            }
        });
        repositorio.deleteById(id);
    }

    public List<Habitacion> obtenerHabitacionesDisponibles(Date checkIn, int noches, Integer excluirRegistroId) {
        List<Habitacion> todasHabitaciones = (List<Habitacion>) habitacionrepositorio.findAll();
        List<Registro> registrosActivos = registrosActivos();

        Date checkOutEstimado = sumarDias(checkIn, noches);

        List<Habitacion> habitacionesOcupadas = registrosActivos.stream()
            .filter(registro -> excluirRegistroId == null || registro.getId() != excluirRegistroId)
            .filter(registro -> seSuperponen(checkIn, checkOutEstimado, registro))
            .map(Registro::getHabitacion)
            .collect(Collectors.toList());

        // La habitación que YA tiene asignada el registro que se está
        // editando siempre tiene que poder elegirse de nuevo, aunque su
        // estado físico actual (LIMPIEZA/MANTENIMIENTO) la excluiría para una
        // reserva nueva. Si no, apenas se hace check-out (la habitación pasa
        // a LIMPIEZA) el selector "pierde" esa habitación al editar ese mismo
        // registro — el resumen muestra "Sin habitación elegida" y, si algo
        // dispara una nueva búsqueda (ej. tocar las fechas), el campo oculto
        // queda expuesto a guardarse vacío.
        Integer habitacionActualId = excluirRegistroId == null ? null
            : repositorio.findById(excluirRegistroId)
                .map(r -> r.getHabitacion() != null ? r.getHabitacion().getId() : null)
                .orElse(null);

        return todasHabitaciones.stream()
            .filter(habitacion -> (habitacionActualId != null && habitacion.getId() == habitacionActualId)
                || (habitacion.getEstado() != EstadoHabitacion.MANTENIMIENTO
                    && habitacion.getEstado() != EstadoHabitacion.LIMPIEZA
                    && !habitacionesOcupadas.contains(habitacion)))
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
        // Recalcular la tarifa con las noches nuevas: si no se hace, la extensión
        // queda cobrada al precio de las noches originales (ver precioParaFecha,
        // misma lógica que usa el check-in para respetar temporada alta).
        double precioPorNoche = registro.getHabitacion().getCategoria().precioParaFecha(registro.getCheckIn());
        registro.setTarifa(precioPorNoche * nuevasNoches);
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

    @Override
    public List<Registro> buscarPorNumeroHabitacion(String query) {
        return repositorio.findByHabitacion_NumeroContainingIgnoreCase(query);
    }

    @Override
    public Set<Integer> idsHabitacionesOcupadasAhora(Date ahora) {
        return registrosActivos().stream()
            .filter(r -> r.getHabitacion() != null && r.getCheckIn() != null)
            .filter(r -> !r.getCheckIn().after(ahora))
            .map(r -> r.getHabitacion().getId())
            .collect(Collectors.toSet());
    }

}
