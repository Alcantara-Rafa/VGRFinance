package com.vgrfinance.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.ItemOrcamento;

/**
 * Divisão exata em centavos, compartilhada pelos métodos orçamentários (RN25).
 *
 * Por que não HALF_EVEN aqui? Dividir 500,00 por 3 e arredondar cada parte para
 * 166,67 soma 500,01: passa da renda. Por isso a parte básica é cortada (DOWN) e
 * os centavos que sobram vão, um a um, às primeiras categorias em ordem
 * alfabética. A soma dá exatamente o total e a diferença entre dois
 * itens nunca passa de um centavo.
 */
final class Centavos {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal UM_CENTAVO = new BigDecimal("0.01");

    private Centavos() {
    }

    /** Percentual de um valor, com 2 casas (arredondamento bancário, RN10). */
    static BigDecimal percentualDe(BigDecimal valor, BigDecimal percentual) {
        return valor.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_EVEN);
    }

    static List<ItemOrcamento> dividirIgualmente(BigDecimal total, List<Categoria> categorias) {
        if (categorias.isEmpty()) {
            return List.of();
        }
        List<Categoria> ordenadas = categorias.stream()
                .sorted(Comparator.comparing(Categoria::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int quantidade = ordenadas.size();
        BigDecimal divisor = BigDecimal.valueOf(quantidade);
        BigDecimal parteBasica = total.divide(divisor, 2, RoundingMode.DOWN);
        int centavosQueSobram = total.subtract(parteBasica.multiply(divisor))
                .movePointRight(2)
                .intValueExact();

        List<ItemOrcamento> itens = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            BigDecimal valor = i < centavosQueSobram ? parteBasica.add(UM_CENTAVO) : parteBasica;
            itens.add(new ItemOrcamento(ordenadas.get(i), valor));
        }
        return itens;
    }
}
