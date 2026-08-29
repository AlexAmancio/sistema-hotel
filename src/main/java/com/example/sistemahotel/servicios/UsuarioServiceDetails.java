package com.example.sistemahotel.servicios;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import com.example.sistemahotel.modelos.Usuario;

public class UsuarioServiceDetails implements UserDetails {
    private final String username;
    private final String password;
    private final boolean activo;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioServiceDetails(Usuario usuario) {
        username = usuario.getUsername();
        password = usuario.getContrasena();
        activo = usuario.isActivo();
        authorities = usuario.isEsAdmin()
            ? AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_STAFF")
            : AuthorityUtils.createAuthorityList("ROLE_STAFF");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
