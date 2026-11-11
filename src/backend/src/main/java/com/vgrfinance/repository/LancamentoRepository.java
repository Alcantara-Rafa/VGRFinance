package com.vgrfinance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.vgrfinance.dto.GastoPorCategoria;
import com.vgrfinance.model.Lancamento;
import com.vgrfinance.model.TipoLancamento;

public interface LancamentoRepository extends JpaRepository<Lancamento, Integer> {

    // O EntityGraph traz a categoria na mesma consulta (evita uma consulta por linha).
    @EntityGraph(attributePaths = "categoria")
    List<Lancamento> findByUsuarioIdOrderByDataDescIdDesc(Integer usuarioId);

    @EntityGraph(attributePaths = "categoria")
    List<Lancamento> findByUsuarioIdAndDataBetweenOrderByDataDescIdDesc(Integer usuarioId,
                                                                        LocalDate inicio, LocalDate fim);

    @EntityGraph(attributePaths = "categoria")
    Optional<Lancamento> findByIdAndUsuarioId(Integer id, Integer usuarioId);

    boolean existsByCategoriaId(Integer categoriaId);

    /** RN07: soma no banco, por categoria, os lançamentos do tipo e do período (as duas pontas entram). */
    @Query("""
           select new com.vgrfinance.dto.GastoPorCategoria(c.id, c.nome, c.essencial, sum(l.valor))
           from Lancamento l join l.categoria c
           where l.usuario.id = :usuarioId
             and l.tipo = :tipo
             and l.data between :inicio and :fim
           group by c.id, c.nome, c.essencial
           """)
    List<GastoPorCategoria> somarPorCategoria(Integer usuarioId, TipoLancamento tipo,
                                              LocalDate inicio, LocalDate fim);
}
