package com.vgrfinance.dominio;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/** Formatação de frações como percentual, só para mensagens de texto (0.1480 vira "14,8%"). */
public final class Percentual {

    private static final Locale BRASIL = Locale.of("pt", "BR");

    private Percentual() {
    }

    public static String formatar(BigDecimal fracao) {
        NumberFormat formato = NumberFormat.getPercentInstance(BRASIL);
        formato.setMaximumFractionDigits(1);
        return formato.format(fracao).replace(' ', ' ');
    }
}
