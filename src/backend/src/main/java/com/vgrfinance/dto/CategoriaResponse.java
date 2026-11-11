package com.vgrfinance.dto;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.TipoCriacao;

public record CategoriaResponse(Integer id, String nome, boolean essencial, TipoCriacao tipoCriacao) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.isEssencial(),
                categoria.getTipoCriacao());
    }
}
