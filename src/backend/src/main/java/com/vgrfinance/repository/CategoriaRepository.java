package com.vgrfinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vgrfinance.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findByUsuarioIdOrderByNomeAsc(Integer usuarioId);

    Optional<Categoria> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCase(Integer usuarioId, String nome);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(Integer usuarioId, String nome, Integer id);
}
