package com.vgrfinance.repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vgrfinance.model.Orcamento;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Integer> {

    // O EntityGraph traz itens e categorias junto, numa consulta só.
    @EntityGraph(attributePaths = {"itens", "itens.categoria"})
    List<Orcamento> findByUsuarioIdOrderByMesReferenciaDesc(Integer usuarioId);

    @EntityGraph(attributePaths = {"itens", "itens.categoria"})
    Optional<Orcamento> findByUsuarioIdAndMesReferencia(Integer usuarioId, YearMonth mes);

    @EntityGraph(attributePaths = {"itens", "itens.categoria"})
    Optional<Orcamento> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    boolean existsByUsuarioIdAndMesReferencia(Integer usuarioId, YearMonth mes);
}
