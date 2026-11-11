package com.vgrfinance.dominio;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatação de dinheiro para MENSAGENS de texto. Nunca use para cálculo. */
public final class Dinheiro {

    private static final Locale BRASIL = Locale.of("pt", "BR");

    private Dinheiro() {
    }

    /** 0.01 vira "R$ 0,01" (com espaço comum no lugar do espaço inquebrável do Java). */
    public static String formatar(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace(' ', ' ');
    }
}
