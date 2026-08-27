package com.example.sistemahotel.servicios;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.example.sistemahotel.interfaces.IUsuarioService;
import com.example.sistemahotel.modelos.Usuario;
import com.example.sistemahotel.repositories.IUsuarioRepository;

@Service
public class UsuarioService implements IUsuarioService, UserDetailsService {

    @Autowired
    private IUsuarioRepository repositorio;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String ip = loginAttemptService.resolveClientIp();
        if (loginAttemptService.isBlocked(ip)) {
            throw new LockedException("Demasiados intentos fallidos. Intente más tarde.");
        }

        Optional<Usuario> usuario = repositorio.findByUsername(username);

        return usuario.map(UsuarioServiceDetails::new).orElseThrow(
            () -> new UsernameNotFoundException("username not found" + username)
        );
    }

    @Override
    public List<Usuario> listar() {
        return (List<Usuario>) repositorio.findAll();
    }

    @Override
    public Optional<Usuario> consultarId(int id) {
        return repositorio.findById(id);
    }

    @Override
    public void guardar(Usuario usuario) {
        repositorio.save(usuario);
    }

    @Override
    public void eliminar(int id) {
        repositorio.deleteById(id);
    }

    @Override
    public Optional <Usuario> consultarpornombreusuario(String username){
        return repositorio.findByUsername(username);
    }

    public void guardarHoraDeCierreDeSesion(String username, LocalDateTime logoutTime) {
        repositorio.findByUsername(username).ifPresent(usuario -> {
            usuario.setLogoutTime(logoutTime);
            repositorio.save(usuario);
        });
    }
}
