package com.vgrfinance.service;

import java.util.List;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Usuario;

/**
 * As categorias que todo cadastro recebe. Único lugar da lista no Java;
 * o database/02_seed.sql repete os mesmos nomes para o usuário de desenvolvimento.
 * Sem "Salário": receita não precisa de categoria.
 */
final class CategoriasPadrao {

    private record Modelo(String nome, boolean essencial) {
    }

    private static final List<Modelo> MODELOS = List.of(
            new Modelo("Moradia", true),
            new Modelo("Alimentação", true),
            new Modelo("Transporte", true),
            new Modelo("Saúde", true),
            new Modelo("Lazer", false),
            new Modelo("Delivery", false),
            new Modelo("Assinaturas", false));

    private CategoriasPadrao() {
    }

    static List<Categoria> para(Usuario usuario) {
        return MODELOS.stream()
                .map(m -> Categoria.padrao(usuario, m.nome(), m.essencial()))
                .toList();
    }
}
