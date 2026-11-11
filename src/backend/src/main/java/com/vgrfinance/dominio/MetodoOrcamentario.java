package com.vgrfinance.dominio;

import java.math.BigDecimal;
import java.util.List;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.ItemOrcamento;
import com.vgrfinance.model.TipoMetodo;

/**
 * Contrato de todo método orçamentário (padrão Strategy).
 * Cada implementação distribui a renda de um jeito; quem usa só chama distribuir().
 *
 * Interface, e não classe abstrata, porque as regras não compartilham atributos
 * nem comportamento: só o contrato.
 */
public interface MetodoOrcamentario {

    /** Código gravado no banco (PROPORCIONAL ou BASE_ZERO). */
    TipoMetodo getTipo();

    /** Devolve um item previsto por categoria, sem passar da renda informada. */
    List<ItemOrcamento> distribuir(BigDecimal rendaBase, List<Categoria> categorias);
}
