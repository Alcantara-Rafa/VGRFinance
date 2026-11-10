package com.vgrfinance.seguranca;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.UsuarioRepository;

/**
 * Dono dos dados de cada requisição. Os serviços perguntam aqui "de quem é?"
 * e nunca leem a sessão diretamente.
 */
@Component
public class UsuarioLogado {

    private final UsuarioRepository usuarios;

    public UsuarioLogado(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Integer id() {
        UsuarioAutenticado autenticado = (UsuarioAutenticado) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return autenticado.getId();
    }

    /** Referência para gravar o dono de um registro, sem consultar o banco. */
    public Usuario obter() {
        return usuarios.getReferenceById(id());
    }
}
