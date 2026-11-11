package com.vgrfinance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.vgrfinance.model.Meta;

public interface MetaRepository extends JpaRepository<Meta, Integer> {

    // Devolve cada meta já como a sua filha (MetaAcumulacao ou MetaReducaoGasto).
    List<Meta> findByUsuarioIdOrderByIdAsc(Integer usuarioId);

    Optional<Meta> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    // A categoria só existe nas metas de redução, por isso a consulta é na filha.
    @Query("select count(m) > 0 from MetaReducaoGasto m where m.categoria.id = :categoriaId")
    boolean existeMetaDaCategoria(Integer categoriaId);
}
