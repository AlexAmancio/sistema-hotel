package com.example.sistemahotel.modelos;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

// Catálogo de productos/servicios que se pueden cargar como consumo extra a
// una estadía (minibar, room service, lavandería, etc.). El precio acá es
// solo el sugerido para precargar el formulario — cada Consumo guarda su
// propio monto, porque lo que realmente se cobra varía caso a caso.
@Data
@Entity
@Table(name = "producto_consumo")
public class ProductoConsumo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;
    private double precioSugerido;
    private boolean activo = true;
}
