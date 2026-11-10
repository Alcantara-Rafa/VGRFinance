package com.vgrfinance.exception;

/** Os dados enviados não fazem sentido para o sistema. Vira HTTP 400. */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
