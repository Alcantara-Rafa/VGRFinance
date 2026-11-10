package com.vgrfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vgrfinance.model.TipoLancamento;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** RN20, lançamento rápido: só o valor é sempre obrigatório; a categoria, só na despesa. */
public record LancamentoRequest(
        @NotNull(message = "Informe o valor.")
        @Positive(message = "O valor precisa ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "Use no máximo 2 casas decimais no valor.")
        BigDecimal valor,

        Integer categoriaId,

        TipoLancamento tipo,

        LocalDate data,

        @Size(max = 120, message = "Use no máximo 120 caracteres na descrição.")
        String descricao) {
}
