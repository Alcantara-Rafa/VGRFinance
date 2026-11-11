package com.vgrfinance.dto;

import java.math.BigDecimal;
import java.util.List;

/** Diagnóstico do mês, calculado na hora e nunca gravado (RN17). */
public record DiagnosticoResponse(
        String mes,
        BigDecimal rendaLiquida,
        BigDecimal despesaTotal,
        BigDecimal sobra,
        List<IndicadorResponse> indicadores) {
}
