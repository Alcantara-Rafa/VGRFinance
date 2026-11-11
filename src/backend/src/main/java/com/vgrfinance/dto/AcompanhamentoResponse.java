package com.vgrfinance.dto;

import java.math.BigDecimal;
import java.util.List;

import com.vgrfinance.model.SituacaoItem;

/** Previsto contra realizado de um mês. Os valores já vêm calculados; a tela só exibe. */
public record AcompanhamentoResponse(
        Integer orcamentoId,
        String mes,
        BigDecimal rendaBase,
        BigDecimal totalPrevisto,
        BigDecimal totalRealizado,
        BigDecimal restanteDaRenda,
        BigDecimal totalExcedentes,
        int itensEstourados,
        List<Item> itens,
        List<ForaDoPlano> foraDoPlano) {

    /** percentualUtilizado é fração (0.8000 = 80%) e é nulo quando o previsto é zero. */
    public record Item(Integer categoriaId, String categoriaNome, boolean essencial,
                       BigDecimal previsto, BigDecimal realizado, BigDecimal desvio, BigDecimal excedente,
                       BigDecimal percentualUtilizado, SituacaoItem situacao) {
    }

    public record ForaDoPlano(Integer categoriaId, String categoriaNome, boolean essencial, BigDecimal realizado) {
    }
}
