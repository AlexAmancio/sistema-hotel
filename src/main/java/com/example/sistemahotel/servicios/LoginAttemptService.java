package com.example.sistemahotel.servicios;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.sistemahotel.modelos.LoginAttempt;
import com.example.sistemahotel.repositories.ILoginAttemptRepository;

import jakarta.servlet.http.HttpServletRequest;


@Service
public class LoginAttemptService {

    @Autowired
    private ILoginAttemptRepository loginAttemptRepository;

    @Value("${security.login.maxAttempts:5}")
    private int maxAttempts;

    @Value("${security.login.lockTimeMinutes:20}")
    private int lockTimeMinutes;

    // Resuelve la IP real del cliente desde el request en curso, en vez de confiar
    // en un valor enviado por el propio formulario (que el cliente podía falsificar).
    public String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void registerAttempt(String ipAddress, String username, boolean successful) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setIpAddress(ipAddress);
        attempt.setUsername(username != null ? username : "unknown"); // Manejar username nulo
        attempt.setAttemptTime(LocalDateTime.now());
        attempt.setSuccessful(successful);
        loginAttemptRepository.save(attempt);
    }

    public boolean isBlocked(String ipAddress) {
        // 1. Obtiene el tiempo límite para la verificación (hace 'lockTimeMinutes' minutos desde ahora).
        //    Se utiliza 'LocalDateTime.now()' para obtener el tiempo actual, y luego se le resta 'lockTimeMinutes'
        //    para determinar el tiempo límite a partir del cual los intentos fallidos serán considerados para el bloqueo.
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(lockTimeMinutes);
        
        // 2. Llama al repositorio (loginAttemptRepository) para contar la cantidad de intentos fallidos
        //    para la dirección IP dada, que hayan ocurrido después del 'cutoffTime'. Esto devuelve el número
        //    de intentos fallidos dentro del intervalo de tiempo determinado.
        long failedAttempts = loginAttemptRepository.countFailedAttempts(ipAddress, cutoffTime);
        
        System.out.println("Tiempo restante" + cutoffTime);

        // 3. Si el número de intentos fallidos es mayor o igual al número máximo permitido de intentos (maxAttempts),
        //    la función devuelve 'true', lo que indica que la IP está bloqueada.
        //    Si no, devuelve 'false', indicando que la IP no está bloqueada.
        return failedAttempts >= maxAttempts;
    }
}
