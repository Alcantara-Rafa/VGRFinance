package com.vgrfinance.dto;

import java.math.BigDecimal;

import com.vgrfinance.model.TipoMeta;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MetaRequest(
        @NotNull(message = "Escolha o tipo da meta.")
        TipoMeta tipo,

        @NotBlank(message = "Descreva a meta.")
        @Size(max = 255, message = "Use no máximo 255 caracteres na descrição.")
        String descricao,

        @NotNull(message = "Informe o valor da meta.")
        @Positive(message = "O valor da meta precisa ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais no valor da meta.")
        BigDecimal valorAlvo,

        // Obrigatória na redução de gasto; vazia na acumulação.
        Integer categoriaId,

        // Só na acumulação e opcional: para quem já tem algum valor guardado.
        @PositiveOrZero(message = "O valor já guardado não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais no valor já guardado.")
        BigDecimal valorAcumulado) {
}
