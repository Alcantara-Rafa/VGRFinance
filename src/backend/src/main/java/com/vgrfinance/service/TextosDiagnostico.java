package com.vgrfinance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.vgrfinance.dominio.Diagnostico;
import com.vgrfinance.dominio.Dinheiro;
import com.vgrfinance.dominio.FaixaDiagnostica;
import com.vgrfinance.dominio.Percentual;
import com.vgrfinance.dto.IndicadorResponse;

/**
 * Textos educativos do diagnóstico: fato, significado e próximo passo, sem culpa.
 * Ficam separados do cálculo para que as frases possam ser revisadas sem tocar nas contas.
 * Faixas sem fonte externa são apresentadas como referência do VGRFinance.
 */
final class TextosDiagnostico {

    private TextosDiagnostico() {
    }

    static IndicadorResponse taxaPoupanca(Diagnostico d) {
        FaixaDiagnostica faixa = d.getFaixaTaxaPoupanca();
        BigDecimal sobra = d.getSobra();
        String percentual = Percentual.formatar(d.getTaxaPoupanca());
        String insuficiente = Percentual.formatar(Diagnostico.LIMITE_POUPANCA_INSUFICIENTE);
        String confortavel = Percentual.formatar(Diagnostico.LIMITE_POUPANCA_CONFORTAVEL);
        String significado;
        String proximoPasso;

        if (sobra.signum() < 0) {
            // RN36: gastou mais do que entrou.
            significado = "Neste mês saíram " + Dinheiro.formatar(sobra.negate()) + " a mais do que entraram. "
                    + "Parte dos gastos foi coberta por dinheiro que não era do mês.";
            proximoPasso = "Veja no acompanhamento quais categorias passaram do previsto e comece pela maior.";
        } else if (faixa == FaixaDiagnostica.SAUDAVEL) {
            significado = "Sobraram " + percentual + " da sua renda (" + Dinheiro.formatar(sobra) + "). "
                    + "Pela referência do VGRFinance, acima de " + confortavel + " é a faixa confortável: "
                    + "dá para formar reserva e perseguir metas sem aperto.";
            proximoPasso = "Dê um destino a essa sobra: crie uma meta de acumulação, como uma reserva de emergência.";
        } else if (faixa == FaixaDiagnostica.ATENCAO) {
            significado = "Sobraram " + percentual + " da sua renda (" + Dinheiro.formatar(sobra) + "). "
                    + "Pela referência do VGRFinance, entre " + insuficiente + " e " + confortavel
                    + " é a faixa adequada: você guarda, mas ainda há espaço para melhorar.";
            proximoPasso = "Para chegar à faixa confortável faltam cerca de " + Dinheiro.formatar(faltaParaConfortavel(d))
                    + " neste mês. Veja se alguma categoria não essencial pode ceder um pouco.";
        } else {
            significado = "Sobraram " + percentual + " da sua renda (" + Dinheiro.formatar(sobra) + "). "
                    + "Pela referência do VGRFinance, abaixo de " + insuficiente
                    + " é a faixa insuficiente: um imprevisto pode virar parcelamento.";
            proximoPasso = "Uma meta de acumulação pequena e fixa é um bom começo, por exemplo "
                    + Dinheiro.formatar(parteDaRenda(d, Diagnostico.LIMITE_POUPANCA_INSUFICIENTE)) + " por mês.";
        }
        String rotulo = switch (faixa) {
            case SAUDAVEL -> "Confortável";
            case ATENCAO -> "Adequada";
            case CRITICA -> "Insuficiente";
        };
        return new IndicadorResponse("TAXA_POUPANCA", "Taxa de poupança", d.getTaxaPoupanca(),
                faixa, rotulo, significado, proximoPasso);
    }

    static IndicadorResponse aderencia(Diagnostico d, BigDecimal totalExcedentes) {
        FaixaDiagnostica faixa = d.getFaixaAderencia();
        String significado = "Você seguiu " + Percentual.formatar(d.getAderenciaOrcamento())
                + " do que planejou. Os gastos acima do previsto somaram " + Dinheiro.formatar(totalExcedentes) + ".";
        String proximoPasso = switch (faixa) {
            case SAUDAVEL -> "Seu plano está próximo da vida real. Mantenha os valores previstos no próximo mês.";
            case ATENCAO -> "Olhe as categorias que passaram do previsto: talvez o valor previsto precise ser mais realista.";
            case CRITICA -> "Isso mostra onde o plano e a vida real se afastaram, não um erro seu. "
                    + "Revise os valores previstos: um plano realista é mais útil do que um plano ideal.";
        };
        String rotulo = switch (faixa) {
            case SAUDAVEL -> "Alta";
            case ATENCAO -> "Média";
            case CRITICA -> "Baixa";
        };
        return new IndicadorResponse("ADERENCIA_ORCAMENTO", "Aderência ao orçamento", d.getAderenciaOrcamento(),
                faixa, rotulo, significado, proximoPasso);
    }

    static IndicadorResponse pesoEssenciais(Diagnostico d) {
        FaixaDiagnostica faixa = d.getFaixaPesoEssenciais();
        String significado = "As despesas essenciais consumiram " + Percentual.formatar(d.getPesoEssenciais())
                + " da sua renda. O método 50/30/20 prevê até "
                + Percentual.formatar(Diagnostico.LIMITE_ESSENCIAIS_EQUILIBRADO) + " para elas.";
        String proximoPasso = switch (faixa) {
            case SAUDAVEL -> "Seus custos essenciais cabem com folga. Isso abre espaço para metas.";
            case ATENCAO -> "Está um pouco acima do que o método prevê, mas ainda é administrável. "
                    + "Acompanhe nos próximos meses.";
            case CRITICA -> "Os custos essenciais deixam pouca margem para poupar. "
                    + "Vale olhar os maiores deles para ver se algum pode ser renegociado ou reduzido com o tempo.";
        };
        String rotulo = switch (faixa) {
            case SAUDAVEL -> "Equilibrado";
            case ATENCAO -> "Elevado";
            case CRITICA -> "Pressionado";
        };
        return new IndicadorResponse("PESO_ESSENCIAIS", "Peso das despesas essenciais", d.getPesoEssenciais(),
                faixa, rotulo, significado, proximoPasso);
    }

    private static BigDecimal parteDaRenda(Diagnostico d, BigDecimal fracao) {
        return d.getRendaLiquida().multiply(fracao).setScale(2, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal faltaParaConfortavel(Diagnostico d) {
        return parteDaRenda(d, Diagnostico.LIMITE_POUPANCA_CONFORTAVEL).subtract(d.getSobra()).max(BigDecimal.ZERO);
    }
}
