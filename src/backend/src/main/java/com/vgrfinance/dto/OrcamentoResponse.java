package com.vgrfinance.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.TipoMetodo;

public record OrcamentoResponse(
        Integer id,
        String mes,
        BigDecimal rendaBase,
        TipoMetodo metodo,
        String metodoNome,
        BigDecimal totalPrevisto,
        BigDecimal reserva,
        boolean zerado,
        List<Item> itens,
        List<String> avisos) {

    public record Item(Integer categoriaId, String categoriaNome, boolean essencial, BigDecimal valorPrevisto) {
    }

    public static OrcamentoResponse de(Orcamento orcamento, List<String> avisos) {
        List<Item> itens = orcamento.getItens().stream()
                .sorted(Comparator.comparing(i -> i.getCategoria().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(i -> {
                    Categoria c = i.getCategoria();
                    return new Item(c.getId(), c.getNome(), c.isEssencial(), i.getValorPrevisto());
                })
                .toList();
        return new OrcamentoResponse(orcamento.getId(), orcamento.getMesReferencia().toString(),
                orcamento.getRendaBase(), orcamento.getMetodo(), orcamento.getMetodo().getNome(),
                orcamento.getTotalPrevisto(), orcamento.getReserva(), orcamento.isZerado(), itens, avisos);
    }
}
