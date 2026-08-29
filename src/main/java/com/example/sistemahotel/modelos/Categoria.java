package com.example.sistemahotel.modelos;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String nombre;
    private String urlImagen;
    private double precio;
    private String descripcion;

    // Ventana de temporada alta (opcional). Si el check-in cae dentro de este
    // rango, se usa precioTemporadaAlta en vez de precio.
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date temporadaAltaInicio;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date temporadaAltaFin;

    private Double precioTemporadaAlta;

    public double precioParaFecha(Date fecha) {
        if (temporadaAltaInicio == null || temporadaAltaFin == null
                || precioTemporadaAlta == null || fecha == null) {
            return precio;
        }
        if (!fecha.before(temporadaAltaInicio) && !fecha.after(temporadaAltaFin)) {
            return precioTemporadaAlta;
        }
        return precio;
    }
}
