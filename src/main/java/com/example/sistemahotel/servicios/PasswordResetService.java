package com.example.sistemahotel.servicios;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.sistemahotel.interfaces.IPasswordResetService;
import com.example.sistemahotel.modelos.PasswordResetToken;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IPasswordResetTokenRepository;
import com.example.sistemahotel.repositories.IUsuarioRepository;

@Service
public class PasswordResetService implements IPasswordResetService {

    @Autowired
    private IUsuarioRepository usuarioRepositorio;

    @Autowired
    private IPasswordResetTokenRepository tokenRepositorio;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${security.reset.tokenExpiracionMinutos:30}")
    private int tokenExpiracionMinutos;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void solicitarReseteo(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByCorreo(correo);
        if (usuarioOpt.isEmpty()) {
            // No se avisa si el correo existe o no, para no revelar qué cuentas son reales.
            return;
        }

        Usuario usuario = usuarioOpt.get();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUsuario(usuario);
        resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(tokenExpiracionMinutos));
        tokenRepositorio.save(resetToken);

        String enlace = baseUrl + "/login/restablecer?token=" + resetToken.getToken();

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(usuario.getCorreo());
        mensaje.setSubject("Restablecer tu contraseña");
        mensaje.setText("Hola " + usuario.getNombres() + ",\n\n"
            + "Pediste restablecer tu contraseña. Abrí este enlace (vale por "
            + tokenExpiracionMinutos + " minutos):\n\n" + enlace
            + "\n\nSi no fuiste tú, ignorá este correo.");

        mailSender.send(mensaje);
    }

    @Override
    public boolean tokenValido(String token) {
        return buscarTokenVigente(token).isPresent();
    }

    @Override
    public boolean restablecerContrasena(String token, String nuevaContrasena) {
        Optional<PasswordResetToken> tokenOpt = buscarTokenVigente(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        Usuario usuario = resetToken.getUsuario();
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepositorio.save(usuario);

        resetToken.setUsado(true);
        tokenRepositorio.save(resetToken);

        return true;
    }

    private Optional<PasswordResetToken> buscarTokenVigente(String token) {
        return tokenRepositorio.findByToken(token)
            .filter(t -> !t.isUsado())
            .filter(t -> t.getFechaExpiracion().isAfter(LocalDateTime.now()));
    }
}
