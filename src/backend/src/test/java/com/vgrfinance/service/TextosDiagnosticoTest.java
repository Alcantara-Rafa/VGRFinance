package com.vgrfinance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.vgrfinance.dominio.Diagnostico;
import com.vgrfinance.dto.IndicadorResponse;

/** Cada indicador gera fato, significado e próximo passo, com as faixas vindas das constantes. */
class TextosDiagnosticoTest {

    private static Diagnostico diagnostico(String despesa, String essenciais, String excedentes) {
        return new Diagnostico(new BigDecimal("3000.00"), new BigDecimal(despesa), new BigDecimal(essenciais),
                new BigDecimal("2400.00"), new BigDecimal(excedentes));
    }

    @Test
    void mesEquilibradoExplicaOsTresIndicadores() {
        Diagnostico d = diagnostico("2550.00", "1350.00", "112.00");

        IndicadorResponse poupanca = TextosDiagnostico.taxaPoupanca(d);
        IndicadorResponse aderencia = TextosDiagnostico.aderencia(d, new BigDecimal("112.00"));
        IndicadorResponse essenciais = TextosDiagnostico.pesoEssenciais(d);

        assertThat(poupanca.rotuloFaixa()).isEqualTo("Adequada");
        assertThat(poupanca.significado()).startsWith("Sobraram 15% da sua renda (R$ 450,00).")
                .contains("Pela referência do VGRFinance, entre 10% e 20%");
        assertThat(poupanca.proximoPasso()).contains("faltam cerca de R$ 150,00");
        assertThat(aderencia.rotuloFaixa()).isEqualTo("Alta");
        assertThat(aderencia.significado())
                .isEqualTo("Você seguiu 95,3% do que planejou. Os gastos acima do previsto somaram R$ 112,00.");
        assertThat(essenciais.rotuloFaixa()).isEqualTo("Equilibrado");
        assertThat(essenciais.significado()).contains("consumiram 45% da sua renda").contains("até 50%");
    }

    @Test
    void deficitTemMensagemPropria() {
        IndicadorResponse poupanca = TextosDiagnostico.taxaPoupanca(diagnostico("3300.00", "1000.00", "300.00"));

        assertThat(poupanca.rotuloFaixa()).isEqualTo("Insuficiente");
        assertThat(poupanca.significado()).startsWith("Neste mês saíram R$ 300,00 a mais do que entraram.");
    }

    @Test
    void faixaConfortavelSugereUmaMeta() {
        IndicadorResponse poupanca = TextosDiagnostico.taxaPoupanca(diagnostico("2000.00", "900.00", "0"));

        assertThat(poupanca.rotuloFaixa()).isEqualTo("Confortável");
        assertThat(poupanca.proximoPasso()).contains("meta de acumulação");
    }

    @Test
    void mesApertadoTemRotulosDeAlerta() {
        Diagnostico d = diagnostico("2900.00", "1900.00", "1000.00");

        assertThat(TextosDiagnostico.taxaPoupanca(d).rotuloFaixa()).isEqualTo("Insuficiente");
        assertThat(TextosDiagnostico.aderencia(d, new BigDecimal("1000.00")).rotuloFaixa()).isEqualTo("Baixa");
        assertThat(TextosDiagnostico.pesoEssenciais(d).rotuloFaixa()).isEqualTo("Pressionado");
    }
}
