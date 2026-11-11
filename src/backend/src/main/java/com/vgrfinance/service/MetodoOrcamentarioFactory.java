package com.vgrfinance.service;

import org.springframework.stereotype.Component;

import com.vgrfinance.dominio.MetodoOrcamentario;
import com.vgrfinance.dominio.RegraBaseZero;
import com.vgrfinance.dominio.RegraProporcional;
import com.vgrfinance.dto.OrcamentoRequest;
import com.vgrfinance.exception.DadosInvalidosException;

/** Escolhe a regra certa. O resto do sistema só enxerga a interface MetodoOrcamentario. */
@Component
public class MetodoOrcamentarioFactory {

    static final String MSG_PERCENTUAIS_INCOMPLETOS = "Informe os três percentuais ou nenhum deles.";

    public MetodoOrcamentario criar(OrcamentoRequest requisicao) {
        return switch (requisicao.metodo()) {
            case PROPORCIONAL -> proporcional(requisicao);
            case BASE_ZERO -> new RegraBaseZero();
        };
    }

    private static RegraProporcional proporcional(OrcamentoRequest r) {
        if (r.pctEssenciais() == null && r.pctNaoEssenciais() == null && r.pctMetas() == null) {
            return new RegraProporcional();
        }
        if (r.pctEssenciais() == null || r.pctNaoEssenciais() == null || r.pctMetas() == null) {
            throw new DadosInvalidosException(MSG_PERCENTUAIS_INCOMPLETOS);
        }
        return new RegraProporcional(r.pctEssenciais(), r.pctNaoEssenciais(), r.pctMetas());
    }
}
