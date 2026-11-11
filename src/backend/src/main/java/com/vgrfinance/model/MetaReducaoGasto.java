package com.vgrfinance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

/** Meta de gastar no máximo um teto por mês numa categoria, por exemplo Delivery até R$ 250,00. */
@Entity
@DiscriminatorValue("REDUCAO_GASTO")
public class MetaReducaoGasto extends Meta {

    public static final String DENTRO_DO_TETO = "DENTRO_DO_TETO";
    public static final String TETO_ULTRAPASSADO = "TETO_ULTRAPASSADO";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    // Sem coluna: depende do mês consultado e dos lançamentos (RN12).
    @Transient
    private BigDecimal gastoDoMes;

    /** Exigido pelo JPA; não use diretamente. */
    protected MetaReducaoGasto() {
    }

    public MetaReducaoGasto(Usuario usuario, String descricao, BigDecimal tetoMensal, Categoria categoria) {
        super(usuario, descricao, tetoMensal);
        this.categoria = categoria;
    }

    public void alterarCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    /** O serviço informa o gasto da categoria no mês consultado antes de pedir progresso e situação. */
    public void apurar(BigDecimal gasto) {
        this.gastoDoMes = gasto;
    }

    /** RN12: fração do teto já consumida (1.0000 = chegou no teto). */
    @Override
    public BigDecimal calcularProgresso() {
        exigirApuracao();
        return gastoDoMes.divide(valorAlvo, 4, RoundingMode.HALF_EVEN);
    }

    /** RN35: o teto só é ultrapassado quando o gasto passa dele; igual ao teto ainda está dentro. */
    @Override
    public String getSituacao() {
        exigirApuracao();
        return gastoDoMes.compareTo(valorAlvo) > 0 ? TETO_ULTRAPASSADO : DENTRO_DO_TETO;
    }

    @Override
    public TipoMeta getTipo() {
        return TipoMeta.REDUCAO_GASTO;
    }

    /** Quanto ainda cabe no teto. */
    public BigDecimal getEconomia() {
        exigirApuracao();
        return valorAlvo.subtract(gastoDoMes).max(BigDecimal.ZERO);
    }

    /** Quanto passou do teto. */
    public BigDecimal getExcedente() {
        exigirApuracao();
        return gastoDoMes.subtract(valorAlvo).max(BigDecimal.ZERO);
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public BigDecimal getGastoDoMes() {
        return gastoDoMes;
    }

    private void exigirApuracao() {
        if (gastoDoMes == null) {
            throw new IllegalStateException("Chame apurar(gasto) antes de calcular a meta.");
        }
    }
}
