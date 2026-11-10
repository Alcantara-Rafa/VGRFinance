package com.vgrfinance.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    /** Exigido pelo JPA; não use diretamente. */
    protected Categoria() {
    }

    public Categoria(Usuario usuario, String nome, boolean essencial) {
        this.usuario = usuario;
        this.nome = nome;
        this.essencial = essencial;
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
}
