package com.vgrfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Lancamento;
import com.vgrfinance.model.TipoLancamento;

public record LancamentoResponse(
        Integer id,
        TipoLancamento tipo,
        BigDecimal valor,
        LocalDate data,
        String descricao,
        Integer categoriaId,
        String categoriaNome) {

    public static LancamentoResponse de(Lancamento lancamento) {
        Categoria categoria = lancamento.getCategoria();
        return new LancamentoResponse(lancamento.getId(), lancamento.getTipo(), lancamento.getValor(),
                lancamento.getData(), lancamento.getDescricao(),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getNome());
    }
}
