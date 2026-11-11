package com.vgrfinance.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.vgrfinance.exception.DadosInvalidosException;

/**
 * Diagnóstico educativo de um mês. Não é entidade: nada aqui é gravado (RN17).
 * Recebe os totais já somados e só faz as contas e a classificação.
 * Todos os indicadores são frações com 4 casas (0.1480 = 14,8%).
 */
public class Diagnostico {

    // Taxa de poupança (RN13): regra da equipe, sem fonte externa.
    public static final BigDecimal LIMITE_POUPANCA_INSUFICIENTE = new BigDecimal("0.10");
    public static final BigDecimal LIMITE_POUPANCA_CONFORTAVEL = new BigDecimal("0.20");
    // Aderência: faixas propostas pela equipe, a validar.
    public static final BigDecimal LIMITE_ADERENCIA_ALTA = new BigDecimal("0.90");
    public static final BigDecimal LIMITE_ADERENCIA_MEDIA = new BigDecimal("0.70");
    // Peso das essenciais: 50% vem do método 50/30/20; 60% é proposta da equipe.
    public static final BigDecimal LIMITE_ESSENCIAIS_EQUILIBRADO = new BigDecimal("0.50");
    public static final BigDecimal LIMITE_ESSENCIAIS_ELEVADO = new BigDecimal("0.60");

    private final BigDecimal rendaLiquida;
    private final BigDecimal despesaTotal;
    private final BigDecimal despesaEssenciais;
    private final BigDecimal totalPrevisto;
    private final BigDecimal totalExcedentes;

    public Diagnostico(BigDecimal rendaLiquida, BigDecimal despesaTotal, BigDecimal despesaEssenciais,
                       BigDecimal totalPrevisto, BigDecimal totalExcedentes) {
        if (rendaLiquida == null || rendaLiquida.signum() <= 0) {
            throw new DadosInvalidosException("A renda do mês precisa ser maior que zero para gerar o diagnóstico.");
        }
        this.rendaLiquida = rendaLiquida;
        this.despesaTotal = despesaTotal;
        this.despesaEssenciais = despesaEssenciais;
        this.totalPrevisto = totalPrevisto;
        this.totalExcedentes = totalExcedentes;
    }

    /** Renda menos despesas. Negativa quando se gastou mais do que entrou. */
    public BigDecimal getSobra() {
        return rendaLiquida.subtract(despesaTotal);
    }

    /** RN13: sobra dividida pela renda líquida. */
    public BigDecimal getTaxaPoupanca() {
        return getSobra().divide(rendaLiquida, 4, RoundingMode.HALF_EVEN);
    }

    /** RN14: previsto menos excedentes, dividido pelo previsto, entre 0 e 1. */
    public BigDecimal getAderenciaOrcamento() {
        if (totalPrevisto.signum() == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        BigDecimal aderencia = totalPrevisto.subtract(totalExcedentes)
                .divide(totalPrevisto, 4, RoundingMode.HALF_EVEN);
        return aderencia.max(BigDecimal.ZERO.setScale(4)).min(BigDecimal.ONE.setScale(4));
    }

    /** RN15: gasto nas categorias essenciais dividido pela renda líquida. */
    public BigDecimal getPesoEssenciais() {
        return despesaEssenciais.divide(rendaLiquida, 4, RoundingMode.HALF_EVEN);
    }

    /** Abaixo de 10% crítica; de 10% a 20% atenção; acima de 20% saudável. */
    public FaixaDiagnostica getFaixaTaxaPoupanca() {
        BigDecimal taxa = getTaxaPoupanca();
        if (taxa.compareTo(LIMITE_POUPANCA_INSUFICIENTE) < 0) {
            return FaixaDiagnostica.CRITICA;
        }
        return taxa.compareTo(LIMITE_POUPANCA_CONFORTAVEL) > 0 ? FaixaDiagnostica.SAUDAVEL : FaixaDiagnostica.ATENCAO;
    }

    /** 90% ou mais saudável; de 70% a 90% atenção; abaixo de 70% crítica. */
    public FaixaDiagnostica getFaixaAderencia() {
        BigDecimal aderencia = getAderenciaOrcamento();
        if (aderencia.compareTo(LIMITE_ADERENCIA_ALTA) >= 0) {
            return FaixaDiagnostica.SAUDAVEL;
        }
        return aderencia.compareTo(LIMITE_ADERENCIA_MEDIA) >= 0 ? FaixaDiagnostica.ATENCAO : FaixaDiagnostica.CRITICA;
    }

    /** Até 50% saudável; até 60% atenção; acima de 60% crítica. */
    public FaixaDiagnostica getFaixaPesoEssenciais() {
        BigDecimal peso = getPesoEssenciais();
        if (peso.compareTo(LIMITE_ESSENCIAIS_EQUILIBRADO) <= 0) {
            return FaixaDiagnostica.SAUDAVEL;
        }
        return peso.compareTo(LIMITE_ESSENCIAIS_ELEVADO) <= 0 ? FaixaDiagnostica.ATENCAO : FaixaDiagnostica.CRITICA;
    }

    public BigDecimal getRendaLiquida() {
        return rendaLiquida;
    }

    public BigDecimal getDespesaTotal() {
        return despesaTotal;
    }
}
