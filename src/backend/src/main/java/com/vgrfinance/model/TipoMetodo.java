package com.vgrfinance.model;

/** Método usado para gerar a distribuição inicial de um orçamento. */
public enum TipoMetodo {
    PROPORCIONAL("Regra proporcional"),
    BASE_ZERO("Base zero");

    private final String nome;

    TipoMetodo(String nome) {
        this.nome = nome;
    }

    /** Nome para mostrar ao usuário. */
    public String getNome() {
        return nome;
    }
}
