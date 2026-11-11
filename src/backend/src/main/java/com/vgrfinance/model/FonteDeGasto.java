package com.vgrfinance.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Quem sabe quanto foi gasto numa categoria em um mês (o serviço, que consulta os lançamentos).
 * A meta de redução usa para se apurar; a de acumulação não precisa.
 */
@FunctionalInterface
public interface FonteDeGasto {

    BigDecimal gastoDaCategoria(Integer categoriaId, YearMonth periodo);
}
