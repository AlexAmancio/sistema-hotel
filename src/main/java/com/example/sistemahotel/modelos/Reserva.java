package com.example.sistemahotel.modelos;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

// Agrupa varios Registro (una habitación cada uno) bajo una sola reserva,
// para una familia o grupo que pide más de un cuarto junto. No reemplaza a
// Registro ni cambia su comportamiento: un Registro individual sin grupo
// (reserva == null) sigue funcionando exactamente igual que antes.
@Data
@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(mappedBy = "reserva", fetch = FetchType.EAGER)
    private List<Registro> registros;

    public double getTotal() {
        if (registros == null) {
            return 0.0;
        }
        return registros.stream()
            .mapToDouble(Registro::getTotalEstadia)
            .sum();
    }
}
