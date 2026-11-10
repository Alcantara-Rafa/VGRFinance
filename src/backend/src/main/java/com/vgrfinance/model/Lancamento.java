package com.vgrfinance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lancamentos")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Opcional só para receita: despesa sempre tem categoria.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDate data;

    @Column(length = 120)
    private String descricao;

    /** Exigido pelo JPA; não use diretamente. */
    protected Lancamento() {
    }

    public Lancamento(Usuario usuario, Categoria categoria, TipoLancamento tipo,
                      BigDecimal valor, LocalDate data, String descricao) {
        this.usuario = usuario;
        atualizar(categoria, tipo, valor, data, descricao);
    }

    public void atualizar(Categoria categoria, TipoLancamento tipo,
                          BigDecimal valor, LocalDate data, String descricao) {
        this.categoria = categoria;
        this.tipo = tipo;
        this.valor = valor;
        this.data = data;
        this.descricao = descricao;
    }

    public Integer getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }
}
