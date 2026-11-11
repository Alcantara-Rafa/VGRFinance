package com.vgrfinance.dto;

import java.math.BigDecimal;
import java.util.List;

import com.vgrfinance.model.TipoMetodo;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record OrcamentoRequest(
        // RN27: o mês chega como texto AAAA-MM e o serviço converte para YearMonth.
        @NotBlank(message = "Informe o mês.")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
                 message = "Informe o mês no formato AAAA-MM, por exemplo 2026-11.")
        String mes,

        @NotNull(message = "Informe a renda do mês.")
        @Positive(message = "A renda deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais na renda.")
        BigDecimal rendaBase,

        @NotNull(message = "Escolha o método.")
        TipoMetodo metodo,

        // Só valem para PROPORCIONAL. Se vierem vazios, vale 50/30/20.
        @PositiveOrZero(message = "Os percentuais não podem ser negativos.")
        @Digits(integer = 3, fraction = 2, message = "Use no máximo 2 casas decimais nos percentuais.")
        BigDecimal pctEssenciais,

        @PositiveOrZero(message = "Os percentuais não podem ser negativos.")
        @Digits(integer = 3, fraction = 2, message = "Use no máximo 2 casas decimais nos percentuais.")
        BigDecimal pctNaoEssenciais,

        @PositiveOrZero(message = "Os percentuais não podem ser negativos.")
        @Digits(integer = 3, fraction = 2, message = "Use no máximo 2 casas decimais nos percentuais.")
        BigDecimal pctMetas,

        // Opcional: se vier vazio, usa todas as categorias do usuário.
        List<Integer> categoriaIds) {
}
