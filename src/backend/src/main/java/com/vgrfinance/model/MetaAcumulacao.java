package com.vgrfinance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.vgrfinance.exception.DadosInvalidosException;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Meta de juntar um valor, por exemplo uma reserva de emergência de R$ 6.000,00. */
@Entity
@DiscriminatorValue("ACUMULACAO")
public class MetaAcumulacao extends Meta {

    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String ATINGIDA = "ATINGIDA";

    @Column(name = "valor_acumulado", precision = 12, scale = 2)
    private BigDecimal valorAcumulado = BigDecimal.ZERO;

    /** Exigido pelo JPA; não use diretamente. */
    protected MetaAcumulacao() {
    }

    public MetaAcumulacao(Usuario usuario, String descricao, BigDecimal valorAlvo) {
        super(usuario, descricao, valorAlvo);
    }

    /** Guarda mais dinheiro na meta. */
    public void registrarAporte(BigDecimal valor) {
        this.valorAcumulado = valorAcumulado.add(valor);
    }

    /** Corrige o valor guardado, por exemplo para desfazer um aporte lançado errado. */
    public void corrigirValorAcumulado(BigDecimal valor) {
        if (valor.signum() < 0) {
            throw new DadosInvalidosException("O valor acumulado não pode ser negativo.");
        }
        this.valorAcumulado = valor;
    }

    /** RN11: acumulado dividido pelo alvo. */
    @Override
    public BigDecimal calcularProgresso() {
        return valorAcumulado.divide(valorAlvo, 4, RoundingMode.HALF_EVEN);
    }

    /** RN35: atingida quando o acumulado alcança ou passa o alvo. */
    @Override
    public String getSituacao() {
        return valorAcumulado.compareTo(valorAlvo) >= 0 ? ATINGIDA : EM_ANDAMENTO;
    }

    @Override
    public TipoMeta getTipo() {
        return TipoMeta.ACUMULACAO;
    }

    public BigDecimal getValorAcumulado() {
        return valorAcumulado;
    }

    /** Quanto falta para o alvo; zero quando já foi atingido. */
    public BigDecimal getFaltante() {
        return valorAlvo.subtract(valorAcumulado).max(BigDecimal.ZERO);
    }
}
