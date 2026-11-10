package com.vgrfinance.dto;

import com.vgrfinance.model.Categoria;

public record CategoriaResponse(Integer id, String nome, boolean essencial) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isEssencial());
    }
}
