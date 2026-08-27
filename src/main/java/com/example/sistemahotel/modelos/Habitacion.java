package com.example.sistemahotel.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;


@Data
@Entity
@Table(name = "habitacion")
public class Habitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String numero;
    private int piso;

    @Enumerated(EnumType.STRING)
    private EstadoHabitacion estado = EstadoHabitacion.DISPONIBLE;

    private String urlImagen;
    private int aforo;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    // Bloqueo optimista: si dos personas editan la misma habitación a la vez,
    // la segunda en guardar recibe un error en vez de pisar en silencio el
    // cambio de la primera (esto es justo lo que causó el bug del formulario
    // viejo sobreescribiendo el estado real de la habitación).
    @Version
    private Long version;

}

