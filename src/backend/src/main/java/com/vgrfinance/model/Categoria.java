package com.vgrfinance.model;

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
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(nullable = false)
    private boolean essencial;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_criacao", nullable = false, length = 12)
    private TipoCriacao tipoCriacao = TipoCriacao.CUSTOMIZADA;

    /** Exigido pelo JPA; não use diretamente. */
    protected Categoria() {
    }

    public Categoria(Usuario usuario, String nome, boolean essencial) {
        this.usuario = usuario;
        this.nome = nome;
        this.essencial = essencial;
    }

    /** Categoria criada pelo sistema no cadastro (ver CategoriasPadrao). */
    public static Categoria padrao(Usuario usuario, String nome, boolean essencial) {
        Categoria categoria = new Categoria(usuario, nome, essencial);
        categoria.tipoCriacao = TipoCriacao.PADRAO;
        return categoria;
    }

    public void atualizar(String nome, boolean essencial) {
        this.nome = nome;
        this.essencial = essencial;
    }

    public Integer getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNome() {
        return nome;
    }

    public boolean isEssencial() {
        return essencial;
    }

    public TipoCriacao getTipoCriacao() {
        return tipoCriacao;
    }

    public boolean isPadrao() {
        return tipoCriacao == TipoCriacao.PADRAO;
    }
}
