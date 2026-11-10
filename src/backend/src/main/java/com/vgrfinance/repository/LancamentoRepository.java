package com.vgrfinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vgrfinance.model.Lancamento;

public interface LancamentoRepository extends JpaRepository<Lancamento, Integer> {

    // O EntityGraph traz a categoria na mesma consulta (evita uma consulta por linha).
    @EntityGraph(attributePaths = "categoria")
    List<Lancamento> findByUsuarioIdOrderByDataDescIdDesc(Integer usuarioId);

    @EntityGraph(attributePaths = "categoria")
    Optional<Lancamento> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    boolean existsByCategoriaId(Integer categoriaId);
}
