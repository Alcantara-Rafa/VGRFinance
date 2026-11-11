package com.vgrfinance.dto;

import java.math.BigDecimal;

import com.vgrfinance.dominio.FaixaDiagnostica;

/** Um indicador do diagnóstico com a explicação pronta: fato, significado e próximo passo. */
public record IndicadorResponse(
        String codigo,
        String titulo,
        BigDecimal valor,
        FaixaDiagnostica faixa,
        String rotuloFaixa,
        String significado,
        String proximoPasso) {
}
