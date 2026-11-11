package com.vgrfinance.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vgrfinance.dominio.Dinheiro;
import com.vgrfinance.dominio.MetodoOrcamentario;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.exception.RegraDeNegocioException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orcamentos")
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "mes_referencia", nullable = false)
    private YearMonth mesReferencia;

    @Column(name = "renda_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal rendaBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMetodo metodo;

    // Composição 1..*: o orçamento é dono dos itens; apagar o orçamento apaga os itens.
    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrcamento> itens = new ArrayList<>();

    /** Exigido pelo JPA; não use diretamente. */
    protected Orcamento() {
    }

    public Orcamento(Usuario usuario, YearMonth mesReferencia, BigDecimal rendaBase) {
        this.usuario = usuario;
        this.mesReferencia = mesReferencia;
        this.rendaBase = rendaBase;
    }

    /**
     * Gera os itens com o método escolhido. Não sabe QUAL método é: só chama
     * distribuir(). Esse é o polimorfismo do padrão Strategy.
     */
    public void gerar(MetodoOrcamentario metodo, List<Categoria> categorias) {
        List<ItemOrcamento> novos = metodo.distribuir(rendaBase, categorias);
        if (novos.isEmpty()) {
            throw new RegraDeNegocioException("O orçamento precisa de ao menos uma categoria.");
        }
        this.metodo = metodo.getTipo();
        this.itens.clear();
        novos.forEach(this::adicionarItem);
    }

    /** RN03 e RN04: o usuário edita o previsto, desde que o total não passe da renda. */
    public void ajustarPrevisto(Integer categoriaId, BigDecimal novoValor) {
        ItemOrcamento item = itens.stream()
                .filter(i -> i.getCategoria().getId().equals(categoriaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esta categoria não faz parte do orçamento."));

        BigDecimal novoTotal = getTotalPrevisto().subtract(item.getValorPrevisto()).add(novoValor);
        if (novoTotal.compareTo(rendaBase) > 0) {
            throw new RegraDeNegocioException("Esse valor ultrapassa a renda do orçamento em "
                    + Dinheiro.formatar(novoTotal.subtract(rendaBase)) + ".");
        }
        item.definirValorPrevisto(novoValor);
    }

    private void adicionarItem(ItemOrcamento item) {
        item.vincularA(this);
        itens.add(item);
    }

    public BigDecimal getTotalPrevisto() {
        return itens.stream()
                .map(ItemOrcamento::getValorPrevisto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** O que sobra da renda sem destino. Não é gravado: é a renda menos o total previsto. */
    public BigDecimal getReserva() {
        return rendaBase.subtract(getTotalPrevisto());
    }

    /** Verdadeiro quando cada real da renda tem destino (ideal da base zero). */
    public boolean isZerado() {
        return getReserva().signum() == 0;
    }

    public Integer getId() {
        return id;
    }

    public YearMonth getMesReferencia() {
        return mesReferencia;
    }

    public BigDecimal getRendaBase() {
        return rendaBase;
    }

    public TipoMetodo getMetodo() {
        return metodo;
    }

    /** Somente leitura: ninguém altera os itens por fora do orçamento. */
    public List<ItemOrcamento> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
