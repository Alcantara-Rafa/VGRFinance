package com.vgrfinance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/** Valor previsto para uma categoria dentro de um orçamento. Só existe dentro dele. */
@Entity
@Table(name = "itens_orcamento")
public class ItemOrcamento {

    /** RN30: a partir de 80% do previsto o item entra em atenção. */
    public static final BigDecimal LIMITE_ATENCAO = new BigDecimal("0.80");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "valor_previsto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPrevisto;

    // Sem coluna: o realizado é somado dos lançamentos na hora da consulta (RN07, RN17).
    @Transient
    private BigDecimal valorRealizado = BigDecimal.ZERO;

    /** Exigido pelo JPA; não use diretamente. */
    protected ItemOrcamento() {
    }

    public ItemOrcamento(Categoria categoria, BigDecimal valorPrevisto) {
        this.categoria = categoria;
        this.valorPrevisto = valorPrevisto;
    }

    /** Só o Orcamento vincula e altera o item (por isso é privado ao pacote). */
    void vincularA(Orcamento orcamento) {
        this.orcamento = orcamento;
    }

    void definirValorPrevisto(BigDecimal valorPrevisto) {
        this.valorPrevisto = valorPrevisto;
    }

    /** O serviço informa o total gasto na categoria no mês antes de consultar desvio e situação. */
    public void informarRealizado(BigDecimal valorRealizado) {
        this.valorRealizado = valorRealizado;
    }

    /** RN29: realizado menos previsto. Positivo quando passou do previsto. */
    public BigDecimal getDesvio() {
        return valorRealizado.subtract(valorPrevisto);
    }

    /** Quanto passou do previsto; zero se não passou. */
    public BigDecimal getExcedente() {
        return getDesvio().max(BigDecimal.ZERO);
    }

    /** Fração do previsto já usada (0.8000 = 80%). Nula quando o previsto é zero. */
    public BigDecimal getPercentualUtilizado() {
        if (valorPrevisto.signum() == 0) {
            return null;
        }
        return valorRealizado.divide(valorPrevisto, 4, RoundingMode.HALF_EVEN);
    }

    /** RN08: passou do previsto quando o realizado é maior; igual ao previsto ainda não passou. */
    public boolean isEstourado() {
        return valorRealizado.compareTo(valorPrevisto) > 0;
    }

    /** Compara com previsto × 80%, e não com o percentual arredondado, para 79,996% não virar atenção. */
    public SituacaoItem getSituacao() {
        if (isEstourado()) {
            return SituacaoItem.ESTOURADO;
        }
        boolean pertoDoLimite = valorPrevisto.signum() > 0
                && valorRealizado.compareTo(valorPrevisto.multiply(LIMITE_ATENCAO)) >= 0;
        return pertoDoLimite ? SituacaoItem.ATENCAO : SituacaoItem.NO_PLANO;
    }

    public Integer getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public BigDecimal getValorPrevisto() {
        return valorPrevisto;
    }

    public BigDecimal getValorRealizado() {
        return valorRealizado;
    }
}
