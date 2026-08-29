package com.example.sistemahotel.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import com.example.sistemahotel.security.CustomAuthenticationFailureHandler;
import com.example.sistemahotel.servicios.LoginAttemptService;
import com.example.sistemahotel.servicios.UsuarioService;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioService usuarioService;
    private final LoginAttemptService loginAttemptService;

    // Inyección por constructor (recomendada para mejor testabilidad) del usuarioService
    public SecurityConfig(UsuarioService usuarioService, LoginAttemptService loginAttemptService) {
        this.usuarioService = usuarioService;
        this.loginAttemptService = loginAttemptService;
    }

    @Bean
    public SecurityFilterChain customfilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/fondologin.png", "/LogoSistema.png", "/favicon.png").permitAll();
                auth.requestMatchers("/login**").permitAll();
                // Cualquier cuenta autenticada puede ver la lista de usuarios,
                // pero solo un administrador puede crear, editar o eliminar
                // (evita que el staff se autopromueva a admin).
                auth.requestMatchers(HttpMethod.GET, "/usuarios/").authenticated();
                auth.requestMatchers("/usuarios/**").hasRole("ADMIN");
                auth.anyRequest().authenticated();
            })
            .formLogin(formLogin -> {
                // Personalizar login
                formLogin.loginPage("/login");
                // URL despues de iniciar sesión
                formLogin.successHandler(successHandler());
                formLogin.failureHandler(customAuthenticationFailureHandlerBean());
                formLogin.permitAll();
            })
            .rememberMe(rememberMe -> {
                rememberMe.key("estadia-remember-me-key");
                rememberMe.tokenValiditySeconds(14 * 24 * 60 * 60);
                rememberMe.userDetailsService(userDetailsService());
            })
            // .authorizeHttpRequests((authorizeHttpRequest) -> {
            //     authorizeHttpRequest.requestMatchers("/**").permitAll();
            // })
            .logout(logout -> {
                // URL donde se debe hacer la solicitud para cerrar sesión
                logout.logoutUrl("/logout");
                // Redirige al usuario a /login después de cerrar sesión.
                logout.logoutSuccessUrl("/login");
                // Usar un handler de logout personalizado para guardar el logout_time en la db
                logout.logoutSuccessHandler(logoutSuccessHandler());
                // Invalida la sesión del usuario
                logout.invalidateHttpSession(true);
                // Elimina la cookie JSESSIONID que está asociada a la sesión del usuario
                logout.deleteCookies("JSESSIONID");
                // Permite que cualquier usuario (autenticado o no) pueda acceder a la URL de logout.
                logout.permitAll();
            })
            .sessionManagement(session -> {
                // Crea una sesión si no existe
                session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
                session.invalidSessionUrl("/login");
                 // Habilitar solo 1 session y el rastreo de usuario autenticado  
                session
                    .maximumSessions(1)
                    .sessionRegistry(sessionRegistry());
                // Que hacer si se apropian del id de sesión (Fijan la sesion)
                session.sessionFixation((sessionFixation) -> {
                    sessionFixation.migrateSession();
                });
            })
            .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();

        authenticationProvider.setUserDetailsService(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());

        return authenticationProvider;
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    public AuthenticationSuccessHandler successHandler() {
        return ((request, response, authentication) -> {
            String ip = loginAttemptService.resolveClientIp();
            loginAttemptService.registerAttempt(ip, authentication.getName(), true);
            response.sendRedirect("/");
        });
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, autentication) -> {

            if (autentication != null) {
                String username = autentication.getName();
                LocalDateTime logoutTime = LocalDateTime.now();
                
                // Guarda el tiempo de logout usando el usuarioService
                usuarioService.guardarHoraDeCierreDeSesion(username, logoutTime);

                // Obten el usuario autenticado
                // System.out.println("Salio un usuario: " + username);
            } 

            // Redirige a la página de login después de hacer logout
            response.sendRedirect("/login?logout");
        };
    }

    @Bean
    public CustomAuthenticationFailureHandler customAuthenticationFailureHandlerBean() {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UsuarioService();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
