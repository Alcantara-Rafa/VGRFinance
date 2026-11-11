package com.vgrfinance.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vgrfinance.exception.DadosInvalidosException;

/** Cenários A, B e C e os limites exatos das faixas. */
class DiagnosticoTest {

    static Diagnostico diagnostico(String renda, String despesa, String essenciais, String previsto, String excedentes) {
        return new Diagnostico(new BigDecimal(renda), new BigDecimal(despesa), new BigDecimal(essenciais),
                new BigDecimal(previsto), new BigDecimal(excedentes));
    }

    @Test
    void cenarioAMesEquilibrado() {
        Diagnostico d = diagnostico("3000.00", "2550.00", "1350.00", "2400.00", "112.00");

        assertThat(d.getTaxaPoupanca()).isEqualByComparingTo("0.1500");
        assertThat(d.getFaixaTaxaPoupanca()).isEqualTo(FaixaDiagnostica.ATENCAO);
        assertThat(d.getAderenciaOrcamento()).isEqualByComparingTo("0.9533");
        assertThat(d.getFaixaAderencia()).isEqualTo(FaixaDiagnostica.SAUDAVEL);
        assertThat(d.getPesoEssenciais()).isEqualByComparingTo("0.4500");
        assertThat(d.getFaixaPesoEssenciais()).isEqualTo(FaixaDiagnostica.SAUDAVEL);
    }

    @Test
    void cenarioBMesApertado() {
        Diagnostico d = diagnostico("3000.00", "2900.00", "1900.00", "2400.00", "1000.00");

        assertThat(d.getTaxaPoupanca()).isEqualByComparingTo("0.0333");
        assertThat(d.getFaixaTaxaPoupanca()).isEqualTo(FaixaDiagnostica.CRITICA);
        assertThat(d.getAderenciaOrcamento()).isEqualByComparingTo("0.5833");
        assertThat(d.getFaixaAderencia()).isEqualTo(FaixaDiagnostica.CRITICA);
        assertThat(d.getPesoEssenciais()).isEqualByComparingTo("0.6333");
        assertThat(d.getFaixaPesoEssenciais()).isEqualTo(FaixaDiagnostica.CRITICA);
    }

    @Test
    void cenarioCDeficit() {
        Diagnostico d = diagnostico("3000.00", "3300.00", "1000.00", "2400.00", "300.00");

        assertThat(d.getSobra()).isEqualByComparingTo("-300.00");
        assertThat(d.getTaxaPoupanca()).isEqualByComparingTo("-0.1000");
        assertThat(d.getFaixaTaxaPoupanca()).isEqualTo(FaixaDiagnostica.CRITICA);
        assertThat(d.getFaixaAderencia()).isEqualTo(FaixaDiagnostica.ATENCAO);
        assertThat(d.getFaixaPesoEssenciais()).isEqualTo(FaixaDiagnostica.SAUDAVEL);
    }

    @ParameterizedTest(name = "despesa {0} -> {1}")
    @CsvSource({
            "2700.00, ATENCAO",
            "2400.00, ATENCAO",
            "2399.70, SAUDAVEL",
            "2700.30, CRITICA",
    })
    void limitesDaTaxaDePoupanca(String despesa, FaixaDiagnostica esperada) {
        Diagnostico d = diagnostico("3000.00", despesa, "0", "2400.00", "0");

        assertThat(d.getFaixaTaxaPoupanca()).isEqualTo(esperada);
    }

    @Test
    void aderenciaNuncaFicaNegativa() {
        assertThat(diagnostico("3000.00", "3000.00", "0", "1000.00", "1500.00").getAderenciaOrcamento())
                .isEqualByComparingTo("0");
    }

    @Test
    void aderenciaEZeroQuandoNadaFoiPrevisto() {
        assertThat(diagnostico("3000.00", "100.00", "0", "0", "100.00").getAderenciaOrcamento())
                .isEqualByComparingTo("0");
    }

    @Test
    void recusaRendaZero() {
        assertThatThrownBy(() -> diagnostico("0", "100.00", "0", "0", "0"))
                .isInstanceOf(DadosInvalidosException.class);
    }
}
