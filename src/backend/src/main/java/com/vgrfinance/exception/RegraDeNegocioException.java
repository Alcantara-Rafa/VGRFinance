package com.vgrfinance.exception;

/** O pedido é válido, mas fere uma regra de negócio. Vira HTTP 409. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
