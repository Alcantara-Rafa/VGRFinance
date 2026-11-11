package com.vgrfinance.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.vgrfinance.exception.DadosInvalidosException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.ItemOrcamento;
import com.vgrfinance.model.TipoMetodo;

/**
 * RN01: divide a renda em percentuais entre essenciais, não essenciais e metas.
 * O padrão é 50/30/20, mas os percentuais são parâmetros: outras proporções
 * não exigem classe nova.
 *
 * A parte das metas NÃO vira item: fica de fora e aparece como reserva
 * (renda menos o total previsto).
 */
public class RegraProporcional implements MetodoOrcamentario {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final BigDecimal pctEssenciais;
    private final BigDecimal pctNaoEssenciais;

    public RegraProporcional() {
        this(new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("20"));
    }

    /** RN26: percentuais não negativos que somam 100. */
    public RegraProporcional(BigDecimal pctEssenciais, BigDecimal pctNaoEssenciais, BigDecimal pctMetas) {
        if (pctEssenciais.signum() < 0 || pctNaoEssenciais.signum() < 0 || pctMetas.signum() < 0) {
            throw new DadosInvalidosException("Os percentuais não podem ser negativos.");
        }
        if (pctEssenciais.add(pctNaoEssenciais).add(pctMetas).compareTo(CEM) != 0) {
            throw new DadosInvalidosException("Os percentuais precisam somar 100.");
        }
        this.pctEssenciais = pctEssenciais;
        this.pctNaoEssenciais = pctNaoEssenciais;
    }

    @Override
    public TipoMetodo getTipo() {
        return TipoMetodo.PROPORCIONAL;
    }

    @Override
    public List<ItemOrcamento> distribuir(BigDecimal rendaBase, List<Categoria> categorias) {
        List<Categoria> essenciais = categorias.stream().filter(Categoria::isEssencial).toList();
        List<Categoria> naoEssenciais = categorias.stream().filter(c -> !c.isEssencial()).toList();

        BigDecimal parteEssenciais = Centavos.percentualDe(rendaBase, pctEssenciais);
        // A trava .min() evita passar da renda por causa de arredondamento (RN04).
        BigDecimal parteNaoEssenciais = Centavos.percentualDe(rendaBase, pctNaoEssenciais)
                .min(rendaBase.subtract(parteEssenciais));

        List<ItemOrcamento> itens = new ArrayList<>(Centavos.dividirIgualmente(parteEssenciais, essenciais));
        itens.addAll(Centavos.dividirIgualmente(parteNaoEssenciais, naoEssenciais));
        return itens;
    }
}
