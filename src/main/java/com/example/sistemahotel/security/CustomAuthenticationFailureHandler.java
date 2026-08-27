package com.example.sistemahotel.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.sistemahotel.servicios.LoginAttemptService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler implements org.springframework.security.web.authentication.AuthenticationFailureHandler {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Value("${security.login.lockTimeMinutes:20}")
    private int lockTimeMinutes;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, 
            HttpServletResponse response, 
            org.springframework.security.core.AuthenticationException exception) 
            throws IOException, ServletException {

        String ipAddress = loginAttemptService.resolveClientIp(); // IP real del request, no la que envía el formulario
        String username = request.getParameter("username"); // Obtener username del formulario (si existe)

        System.out.println("FALLIDO: El username es: " + username);

        // Registrar el intento fallido
        loginAttemptService.registerAttempt(ipAddress, username, false);

        String messageError;

        if (loginAttemptService.isBlocked(ipAddress)) {
            messageError = "Vuelva a intentar en " + lockTimeMinutes + "minutos";
        } else {
            messageError = "Inicio de sesión fallido. Verifique sus credenciales.";
        }

        // Redirigir si ya estamos en la página de login
        response.sendRedirect(
            "/login?error=" + URLEncoder.encode(messageError, StandardCharsets.UTF_8.toString())
        );
    }
}
