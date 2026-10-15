package com.example.sistemahotel.interfaces;

public interface IPasswordResetService {
    public void solicitarReseteo(String correo);
    public boolean tokenValido(String token);
    public boolean restablecerContrasena(String token, String nuevaContrasena);
}
