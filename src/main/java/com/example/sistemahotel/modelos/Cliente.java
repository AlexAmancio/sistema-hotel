package com.example.sistemahotel.modelos;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String nombres;
    private String apellidos;
    private String pais;
    private String ciudad;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento = TipoDocumento.DNI;

    // Defecto D13: la base de datos también impide documentos repetidos.
    @Column(unique = true)
    private String identificacion;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fechaNacimiento;
    private String telefono;
    private String correo;
    private String ocupacion;
}
