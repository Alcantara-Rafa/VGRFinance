package com.vgrfinance.seguranca;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.vgrfinance.repository.UsuarioRepository;

/** Ensina o Spring Security a buscar o usuário pelo e-mail na hora do login. */
@Service
public class CarregadorDeUsuarios implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public CarregadorDeUsuarios(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .map(UsuarioAutenticado::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
