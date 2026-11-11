package com.vgrfinance.model;

/**
 * Quem criou a categoria. É também o valor da coluna tipo_criacao.
 * PADRAO: criada pelo sistema no cadastro; não pode ser excluída nem renomeada, só o "essencial" muda.
 * CUSTOMIZADA: criada pela pessoa; aceita qualquer alteração.
 */
public enum TipoCriacao {
    PADRAO,
    CUSTOMIZADA
}
