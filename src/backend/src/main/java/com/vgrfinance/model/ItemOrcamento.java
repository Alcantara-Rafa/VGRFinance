package com.vgrfinance.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Valor previsto para uma categoria dentro de um orçamento. Só existe dentro dele. */
@Entity
@Table(name = "itens_orcamento")
public class ItemOrcamento {

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

    public Integer getId() {
        return id;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public BigDecimal getValorPrevisto() {
        return valorPrevisto;
    }
}
