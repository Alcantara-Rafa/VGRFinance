package com.vgrfinance.dominio;

import java.math.BigDecimal;
import java.util.List;

import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.ItemOrcamento;
import com.vgrfinance.model.TipoMetodo;

/**
 * RN02, base zero: cada real da renda recebe um destino. A sugestão inicial divide
 * a renda inteira, em partes iguais, entre as categorias; o usuário então ajusta
 * os valores.
 */
public class RegraBaseZero implements MetodoOrcamentario {

    @Override
    public TipoMetodo getTipo() {
        return TipoMetodo.BASE_ZERO;
    }

    @Override
    public List<ItemOrcamento> distribuir(BigDecimal rendaBase, List<Categoria> categorias) {
        return Centavos.dividirIgualmente(rendaBase, categorias);
    }
}
