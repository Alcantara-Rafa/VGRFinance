package com.vgrfinance.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AjusteItemRequest(
        @NotNull(message = "Informe o valor previsto.")
        @PositiveOrZero(message = "O valor previsto não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais.")
        BigDecimal valorPrevisto) {
}
