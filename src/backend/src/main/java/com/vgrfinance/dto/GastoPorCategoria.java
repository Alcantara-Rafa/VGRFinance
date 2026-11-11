package com.vgrfinance.dto;

import java.math.BigDecimal;

/** Total gasto numa categoria dentro de um período, somado pelo banco. */
public record GastoPorCategoria(Integer categoriaId, String categoriaNome, boolean essencial, BigDecimal total) {
}
