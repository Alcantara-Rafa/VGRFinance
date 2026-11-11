package com.vgrfinance.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Limites de atenção (80%) e de estouro (passar do previsto). */
class ItemOrcamentoTest {

    private static ItemOrcamento item(String previsto, String realizado) {
        Categoria categoria = new Categoria(new Usuario("Teste", "teste@vgr.local", "hash"), "Delivery", false);
        ItemOrcamento item = new ItemOrcamento(categoria, new BigDecimal(previsto));
        item.informarRealizado(new BigDecimal(realizado));
        return item;
    }

    @ParameterizedTest(name = "previsto {0}, realizado {1} -> {2}")
    @CsvSource({
            "300.00, 412.00, ESTOURADO",
            "300.00, 240.00, ATENCAO",
            "300.00, 239.99, NO_PLANO",
            "300.00, 300.00, ATENCAO",
            "0.00,   10.00,  ESTOURADO",
            "0.00,   0.00,   NO_PLANO",
    })
    void classificaASituacaoNosLimites(String previsto, String realizado, SituacaoItem esperada) {
        assertThat(item(previsto, realizado).getSituacao()).isEqualTo(esperada);
    }

    @Test
    void calculaDesvioExcedenteEPercentualQuandoPassa() {
        ItemOrcamento delivery = item("300.00", "412.00");

        assertThat(delivery.getDesvio()).isEqualByComparingTo("112.00");
        assertThat(delivery.getExcedente()).isEqualByComparingTo("112.00");
        assertThat(delivery.getPercentualUtilizado()).isEqualByComparingTo("1.3733");
        assertThat(delivery.isEstourado()).isTrue();
    }

    @Test
    void igualAoPrevistoNaoPassa() {
        assertThat(item("300.00", "300.00").isEstourado()).isFalse();
    }

    @Test
    void excedenteNuncaENegativo() {
        ItemOrcamento lazer = item("300.00", "150.00");

        assertThat(lazer.getDesvio()).isEqualByComparingTo("-150.00");
        assertThat(lazer.getExcedente()).isEqualByComparingTo("0");
    }

    @Test
    void percentualENuloQuandoOPrevistoEZero() {
        assertThat(item("0.00", "10.00").getPercentualUtilizado()).isNull();
    }
}
