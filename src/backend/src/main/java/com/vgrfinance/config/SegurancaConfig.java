package com.vgrfinance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Login por sessão com cookie. O React e a API ficam no mesmo endereço,
 * então o navegador reenvia o cookie sozinho. O CSRF usa o modo SPA:
 * o token vem no cookie XSRF-TOKEN e volta no cabeçalho X-XSRF-TOKEN.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, SecurityContextRepository contextos) {
        return http
                .csrf(csrf -> csrf.spa())
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/cadastro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                // Sem login, a API responde 401 em vez de redirecionar para uma página.
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .securityContext(contexto -> contexto.securityContextRepository(contextos))
                .logout(sair -> sair
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                .build();
    }

    @Bean
    public SecurityContextRepository contextos() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager gerenciadorDeAutenticacao(UserDetailsService carregador,
                                                           PasswordEncoder codificador) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(carregador);
        provedor.setPasswordEncoder(codificador);
        return new ProviderManager(provedor);
    }
}
