package com.example.sistemahotel.modelos;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Data
@Entity
@Table(name = "registro")
public class Registro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date checkIn;

    private int noches;
    private Double tarifa;
    private int numHuespedes;
    private String observaciones;

    @Enumerated(EnumType.STRING)
    private EstadoRegistro estado = EstadoRegistro.ACTIVA;

    // Fecha real en la que el huésped entregó la habitación. Null mientras la
    // estadía sigue activa: es lo que distingue "ya pasó la fecha calculada"
    // (calcularCheckoutEstimado) de "el huésped realmente se fue".
    private Date checkOutReal;

    private Double montoPagado = 0.0;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    @ManyToOne
    @JoinColumn(name="cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name="habitacion_id")
    private Habitacion habitacion;

    @ManyToOne
    @JoinColumn(name="usuario_id")
    private Usuario usuario;

    // Null para un registro individual (el caso de siempre). Si no es null,
    // este registro es una de varias habitaciones de una misma Reserva
    // (grupo familiar, etc.) — ver Reserva.java.
    @ManyToOne
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @OneToMany(mappedBy = "registro", fetch = FetchType.EAGER)
    private List<Consumo> consumos;

    // Bloqueo optimista: ver Habitacion.version.
    @Version
    private Long version;

    public double getTotalConsumos() {
        if (consumos == null) {
            return 0.0;
        }
        return consumos.stream().mapToDouble(Consumo::getMonto).sum();
    }

    public double getTotalEstadia() {
        double tarifaBase = tarifa != null ? tarifa : 0.0;
        return tarifaBase + getTotalConsumos();
    }

    public double getSaldoPendiente() {
        double pagado = montoPagado != null ? montoPagado : 0.0;
        return getTotalEstadia() - pagado;
    }


    public Date calcularCheckoutEstimado() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(checkIn);

        // Ajustar la hora de check-in a las 12:00 del día de entrada
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Sumar el número de noches al checkout estimado
        calendar.add(Calendar.DAY_OF_MONTH, noches);

        return calendar.getTime();
    }

    public String calcularCheckoutEstimadoAmigable() {
        Calendar now = Calendar.getInstance();
        Calendar checkout = Calendar.getInstance();
        checkout.setTime(checkIn);

        // Ajustar la hora de check-in a las 12:00 del día de entrada
        checkout.set(Calendar.HOUR_OF_DAY, 12);
        checkout.set(Calendar.MINUTE, 0);
        checkout.set(Calendar.SECOND, 0);
        checkout.set(Calendar.MILLISECOND, 0);

        // Sumar el número de noches al checkout estimado
        checkout.add(Calendar.DAY_OF_MONTH, noches);

        // Calcular la diferencia en milisegundos
        long diferenciaMillis = checkout.getTimeInMillis() - now.getTimeInMillis();

        // Verificar si la diferencia es negativa (vencido)
        if (diferenciaMillis < 0) {
            return "Vencido";
        }

        // Calcular la diferencia en días, horas, minutos y segundos
        long dias = diferenciaMillis / (24 * 60 * 60 * 1000);
        long horas = (diferenciaMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutos = (diferenciaMillis % (60 * 60 * 1000)) / (60 * 1000);
        StringBuilder resultado = new StringBuilder();
        if (dias > 0) {
            resultado.append(dias).append(" días ");
        }
        resultado.append(horas).append(" h ").append(minutos).append(" m "); //.append(segundos).append(" s");

        return resultado.toString();
    }
}
