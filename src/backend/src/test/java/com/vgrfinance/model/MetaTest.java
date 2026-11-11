package com.vgrfinance.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vgrfinance.exception.DadosInvalidosException;

/** Classe abstrata Meta, herança nas duas filhas e polimorfismo com @Override. */
class MetaTest {

    private final Usuario usuario = new Usuario("Teste", "teste@vgr.local", "hash");
    private final Categoria delivery = new Categoria(usuario, "Delivery", false);

    private MetaAcumulacao reserva(String acumulado) {
        MetaAcumulacao meta = new MetaAcumulacao(usuario, "Reserva", new BigDecimal("6000.00"));
        meta.corrigirValorAcumulado(new BigDecimal(acumulado));
        return meta;
    }

    private MetaReducaoGasto deliveryAte250(String gasto) {
        MetaReducaoGasto meta = new MetaReducaoGasto(usuario, "Delivery até 250", new BigDecimal("250.00"), delivery);
        meta.apurar(new BigDecimal(gasto));
        return meta;
    }

    @Test
    void acumulacaoEmAndamentoMostraProgressoEFaltante() {
        MetaAcumulacao meta = reserva("2400.00");

        assertThat(meta.calcularProgresso()).isEqualByComparingTo("0.4000");
        assertThat(meta.getSituacao()).isEqualTo(MetaAcumulacao.EM_ANDAMENTO);
        assertThat(meta.getFaltante()).isEqualByComparingTo("3600.00");
    }

    @Test
    void acumulacaoAtingidaQuandoAlcancaOAlvo() {
        MetaAcumulacao meta = reserva("6000.00");

        assertThat(meta.calcularProgresso()).isEqualByComparingTo("1.0000");
        assertThat(meta.getSituacao()).isEqualTo(MetaAcumulacao.ATINGIDA);
        assertThat(meta.getFaltante()).isEqualByComparingTo("0");
    }

    @Test
    void aporteSomaAoAcumulado() {
        MetaAcumulacao meta = reserva("2400.00");

        meta.registrarAporte(new BigDecimal("600.00"));

        assertThat(meta.getValorAcumulado()).isEqualByComparingTo("3000.00");
    }

    @Test
    void recusaCorrigirParaValorNegativo() {
        MetaAcumulacao meta = reserva("100.00");

        assertThatThrownBy(() -> meta.corrigirValorAcumulado(new BigDecimal("-1")))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void reducaoDentroDoTetoMostraEconomia() {
        MetaReducaoGasto meta = deliveryAte250("200.00");

        assertThat(meta.calcularProgresso()).isEqualByComparingTo("0.8000");
        assertThat(meta.getSituacao()).isEqualTo(MetaReducaoGasto.DENTRO_DO_TETO);
        assertThat(meta.getEconomia()).isEqualByComparingTo("50.00");
        assertThat(meta.getExcedente()).isEqualByComparingTo("0");
    }

    @Test
    void reducaoExatamenteNoTetoAindaEstaDentro() {
        assertThat(deliveryAte250("250.00").getSituacao()).isEqualTo(MetaReducaoGasto.DENTRO_DO_TETO);
    }

    @Test
    void reducaoAcimaDoTetoMostraExcedente() {
        MetaReducaoGasto meta = deliveryAte250("310.00");

        assertThat(meta.calcularProgresso()).isEqualByComparingTo("1.2400");
        assertThat(meta.getSituacao()).isEqualTo(MetaReducaoGasto.TETO_ULTRAPASSADO);
        assertThat(meta.getExcedente()).isEqualByComparingTo("60.00");
    }

    @Test
    void reducaoSemApuracaoNaoCalcula() {
        MetaReducaoGasto meta = new MetaReducaoGasto(usuario, "Delivery até 250", new BigDecimal("250.00"), delivery);

        assertThatThrownBy(meta::calcularProgresso).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mesmaChamadaRespondeDoJeitoDeCadaFilha() {
        List<Meta> metas = List.of(reserva("6000.00"), deliveryAte250("310.00"));

        List<String> situacoes = metas.stream().map(meta -> meta.getTipo() + " -> " + meta.getSituacao()).toList();

        assertThat(situacoes).containsExactly("ACUMULACAO -> ATINGIDA", "REDUCAO_GASTO -> TETO_ULTRAPASSADO");
    }
}
