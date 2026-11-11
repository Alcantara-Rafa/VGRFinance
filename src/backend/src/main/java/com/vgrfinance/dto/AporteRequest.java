package com.vgrfinance.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** RN32: o aporte é maior que zero. */
public record AporteRequest(
        @NotNull(message = "Informe o valor do aporte.")
        @Positive(message = "O aporte precisa ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais no aporte.")
        BigDecimal valor) {
}
