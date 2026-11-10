package com.vgrfinance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vgrfinance.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmailIgnoreCase(String email);
}
