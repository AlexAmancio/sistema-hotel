package com.example.sistemahotel.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sistemahotel.modelos.PasswordResetToken;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IPasswordResetTokenRepository;

// Recuperación de contraseña por correo (HU03 y HU04). Reglas RN19 y RN36.
class RecuperacionContrasenaFeatureTest extends FeatureTestCase {

    @Autowired
    private IPasswordResetTokenRepository tokens;

    private PasswordResetToken token(Usuario usuario, String valor, LocalDateTime vence, boolean usado) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUsuario(usuario);
        t.setToken(valor);
        t.setFechaExpiracion(vence);
        t.setUsado(usado);
        return tokens.save(t);
    }

    @Test
    void la_pantalla_de_recuperacion_es_publica() throws Exception {
        assertOk(comoInvitado().get("/login/olvide-contrasena"));
    }

    @Test
    void con_un_correo_registrado_se_crea_el_token_y_se_envia_el_correo() throws Exception {
        Usuario usuario = crearRecepcionista("qa_recepcion");

        MvcResult respuesta = comoInvitado().post("/login/olvide-contrasena", "correo", "qa_recepcion@tunki.test");

        assertOk(respuesta);
        assertEquals(true, modelo(respuesta, "enviado"));
        assertDatabaseHas("password_reset_token", Map.of("usuario_id", usuario.getId(), "usado", false));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void con_un_correo_que_no_existe_no_se_crea_token_pero_se_muestra_el_mismo_aviso() throws Exception {
        int antes = contar("password_reset_token", Map.of());

        MvcResult respuesta = comoInvitado().post("/login/olvide-contrasena", "correo", "nadie@tunki.test");

        assertEquals(true, modelo(respuesta, "enviado"));
        assertEquals(antes, contar("password_reset_token", Map.of()));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void un_enlace_vigente_muestra_el_formulario() throws Exception {
        token(crearRecepcionista("qa_recepcion"), "token-vigente-qa", LocalDateTime.now().plusMinutes(30), false);

        assertEquals("formulario", modelo(comoInvitado().get("/login/restablecer", "token", "token-vigente-qa"), "estado"));
    }

    @Test
    void un_enlace_vencido_se_muestra_como_invalido() throws Exception {
        token(crearRecepcionista("qa_recepcion"), "token-vencido-qa", LocalDateTime.now().minusMinutes(1), false);

        assertEquals("invalido", modelo(comoInvitado().get("/login/restablecer", "token", "token-vencido-qa"), "estado"));
    }

    @Test
    void restablecer_cambia_la_contrasena_y_marca_el_enlace_como_usado() throws Exception {
        Usuario usuario = crearRecepcionista("qa_recepcion");
        token(usuario, "token-bueno-qa", LocalDateTime.now().plusMinutes(30), false);

        MvcResult respuesta = comoInvitado().post("/login/restablecer", "token", "token-bueno-qa", "nuevaContrasena", "Nueva2026");

        assertEquals("exito", modelo(respuesta, "estado"));
        assertTrue(passwordEncoder.matches("Nueva2026", columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, usuario.getId())));
        assertDatabaseHas("password_reset_token", Map.of("token", "token-bueno-qa", "usado", true));
    }

    @Test
    void un_enlace_ya_usado_no_vuelve_a_cambiar_la_contrasena() throws Exception {
        Usuario usuario = crearRecepcionista("qa_recepcion");
        String hashAnterior = columna("SELECT contrasena FROM usuario WHERE id = ?", String.class, usuario.getId());
        token(usuario, "token-usado-qa", LocalDateTime.now().plusMinutes(30), true);

        MvcResult respuesta = comoInvitado().post("/login/restablecer", "token", "token-usado-qa", "nuevaContrasena", "Otra2026");

        assertEquals("invalido", modelo(respuesta, "estado"));
        assertDatabaseHas("usuario", Map.of("id", usuario.getId(), "contrasena", hashAnterior));
    }
}
