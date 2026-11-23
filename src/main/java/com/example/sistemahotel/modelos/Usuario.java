package com.example.sistemahotel.modelos;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Data
@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String nombres;
    private String apellidos;

    // Defecto D13: la base de datos también impide nombres de usuario repetidos.
    @Column(unique = true)
    private String username;
    private String correo;
    private String contrasena;
    private boolean esAdmin;
    private boolean activo = true;

    private LocalDateTime logoutTime;
}
