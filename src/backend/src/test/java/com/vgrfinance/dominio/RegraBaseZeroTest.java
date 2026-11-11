package com.vgrfinance.dominio;

import static com.vgrfinance.dominio.RegraProporcionalTest.categoria;
import static com.vgrfinance.dominio.RegraProporcionalTest.total;
import static com.vgrfinance.dominio.RegraProporcionalTest.valores;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.ItemOrcamento;

class RegraBaseZeroTest {

    private final RegraBaseZero regra = new RegraBaseZero();

    @Test
    void divide1000PorTresSemPerderCentavo() {
        List<ItemOrcamento> itens = regra.distribuir(new BigDecimal("1000.00"),
                List.of(categoria("C", true), categoria("A", false), categoria("B", true)));

        assertThat(valores(itens)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "A", "333.34", "B", "333.33", "C", "333.33"));
        assertThat(total(itens)).isEqualByComparingTo("1000.00");
    }

    @Test
    void divide2500PorSete() {
        List<Categoria> sete = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            sete.add(categoria("Categoria " + i, i % 2 == 0));
        }

        List<ItemOrcamento> itens = regra.distribuir(new BigDecimal("2500.00"), sete);

        assertThat(itens).filteredOn(i -> i.getValorPrevisto().compareTo(new BigDecimal("357.15")) == 0).hasSize(2);
        assertThat(itens).filteredOn(i -> i.getValorPrevisto().compareTo(new BigDecimal("357.14")) == 0).hasSize(5);
        assertThat(total(itens)).isEqualByComparingTo("2500.00");
    }

    @Test
    void sempreFechaExatoComDiferencaMaximaDeUmCentavo() {
        Random sorteio = new Random(7);
        for (int caso = 0; caso < 20_000; caso++) {
            BigDecimal renda = BigDecimal.valueOf(1 + sorteio.nextInt(2_000_000), 2);
            List<Categoria> categorias = new ArrayList<>();
            int quantidade = 1 + sorteio.nextInt(9);
            for (int i = 0; i < quantidade; i++) {
                categorias.add(categoria("C" + i, sorteio.nextBoolean()));
            }

            List<ItemOrcamento> itens = regra.distribuir(renda, categorias);
            BigDecimal maior = itens.stream().map(ItemOrcamento::getValorPrevisto).max(BigDecimal::compareTo).orElseThrow();
            BigDecimal menor = itens.stream().map(ItemOrcamento::getValorPrevisto).min(BigDecimal::compareTo).orElseThrow();

            assertThat(total(itens)).as("renda %s", renda).isEqualByComparingTo(renda);
            assertThat(maior.subtract(menor)).isLessThanOrEqualTo(new BigDecimal("0.01"));
        }
    }
}
