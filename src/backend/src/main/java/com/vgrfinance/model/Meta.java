package com.vgrfinance.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Classe abstrata: define o que toda meta sabe fazer, mas não diz como.
 * Cada filha calcula progresso e situação do seu jeito (polimorfismo).
 *
 * Abstrata, e não interface, porque as filhas compartilham atributos
 * (descrição e valor alvo) e comportamento (atualizarBase).
 * As duas filhas ficam na tabela metas; a coluna tipo_meta diz qual é qual.
 */
@Entity
@Table(name = "metas")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_meta", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Meta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    protected String descricao;

    // Valor a juntar (acumulação) ou teto mensal (redução de gasto).
    @Column(name = "valor_alvo", nullable = false, precision = 12, scale = 2)
    protected BigDecimal valorAlvo;

    /** Exigido pelo JPA; não use diretamente. */
    protected Meta() {
    }

    protected Meta(Usuario usuario, String descricao, BigDecimal valorAlvo) {
        this.usuario = usuario;
        this.descricao = descricao;
        this.valorAlvo = valorAlvo;
    }

    /** Fração do caminho percorrido, com 4 casas (0.4000 = 40%). */
    public abstract BigDecimal calcularProgresso();

    /** Código da situação (por exemplo ATINGIDA). A tela transforma em texto, ícone e cor. */
    public abstract String getSituacao();

    public abstract TipoMeta getTipo();

    /** Troca descrição e alvo. Cada filha cuida do que é só dela. */
    public void atualizarBase(String descricao, BigDecimal valorAlvo) {
        this.descricao = descricao;
        this.valorAlvo = valorAlvo;
    }

    public Integer getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorAlvo() {
        return valorAlvo;
    }
}
