package com.vgrfinance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vgrfinance.model.ItemOrcamento;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Integer> {

    boolean existsByCategoriaId(Integer categoriaId);
}
