package com.vgrfinance.dto;

import java.math.BigDecimal;

import com.vgrfinance.model.TipoMeta;

/**
 * Meta com progresso e situação já calculados. Os campos que não se aplicam ao tipo vêm nulos:
 * acumulação usa valorAcumulado e faltante; redução usa categoria, mês, gasto, economia e excedente.
 */
public record MetaResponse(
        Integer id,
        TipoMeta tipo,
        String descricao,
        BigDecimal valorAlvo,
        BigDecimal progresso,
        String situacao,
        BigDecimal valorAcumulado,
        BigDecimal faltante,
        Integer categoriaId,
        String categoriaNome,
        String mes,
        BigDecimal gastoDoMes,
        BigDecimal economia,
        BigDecimal excedente) {
}
