package com.example.sistemahotel.modelos;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

// Comprobante "local": tiene la forma de un comprobante electrónico peruano
// (serie-correlativo, IGV desglosado, snapshot en JSON) pero no está firmado
// ni fue enviado a SUNAT. Es la base para conectar un OSE real (ej. Nubefact)
// más adelante sin tener que rediseñar el modelo de datos.
@Data
@Entity
@Table(name = "comprobante")
public class Comprobante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String serie;
    private int correlativo;
    private LocalDateTime fechaEmision;

    private Double valorVenta;
    private Double igv;
    private Double total;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String datosJson;

    @OneToOne
    @JoinColumn(name = "registro_id", unique = true)
    private Registro registro;

    public String getNumeroCompleto() {
        return serie + "-" + String.format("%08d", correlativo);
    }
}
