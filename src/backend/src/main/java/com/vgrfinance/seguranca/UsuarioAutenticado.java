package com.vgrfinance.seguranca;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.vgrfinance.model.Usuario;

/** O usuário guardado na sessão depois do login. Não carrega a entidade JPA. */
public class UsuarioAutenticado implements UserDetails {

    private final Integer id;
    private final String nome;
    private final String email;
    private final String senhaHash;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.senhaHash = usuario.getSenhaHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Sem papéis: todo usuário autenticado tem o mesmo acesso, sempre aos próprios dados.
        return List.of();
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public Integer getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
