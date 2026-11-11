package com.vgrfinance.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.vgrfinance.dominio.RegraBaseZero;
import com.vgrfinance.dominio.RegraProporcional;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.exception.RegraDeNegocioException;

class OrcamentoTest {

    private final Usuario usuario = new Usuario("Teste", "teste@vgr.local", "hash");
    private List<Categoria> categorias;
    private Orcamento orcamento;

    private Categoria categoria(int id, String nome, boolean essencial) {
        Categoria categoria = new Categoria(usuario, nome, essencial);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    @BeforeEach
    void preparar() {
        categorias = List.of(categoria(1, "Moradia", true), categoria(2, "Alimentação", true),
                categoria(3, "Lazer", false), categoria(4, "Delivery", false), categoria(5, "Assinaturas", false));
        orcamento = new Orcamento(usuario, YearMonth.of(2026, 11), new BigDecimal("3000.00"));
    }

    @Test
    void geraComARegraProporcionalEMostraAReserva() {
        orcamento.gerar(new RegraProporcional(), categorias);

        assertThat(orcamento.getMetodo()).isEqualTo(TipoMetodo.PROPORCIONAL);
        assertThat(orcamento.getItens()).hasSize(5);
        assertThat(orcamento.getTotalPrevisto()).isEqualByComparingTo("2400.00");
        assertThat(orcamento.getReserva()).isEqualByComparingTo("600.00");
        assertThat(orcamento.isZerado()).isFalse();
    }

    @Test
    void geraComABaseZeroEFicaZerado() {
        orcamento.gerar(new RegraBaseZero(), categorias);

        assertThat(orcamento.getMetodo()).isEqualTo(TipoMetodo.BASE_ZERO);
        assertThat(orcamento.isZerado()).isTrue();
    }

    @Test
    void ajustarAteARendaZeraAReserva() {
        orcamento.gerar(new RegraProporcional(), categorias);

        orcamento.ajustarPrevisto(3, new BigDecimal("900.00"));    // Lazer: 300 + 600

        assertThat(orcamento.getReserva()).isEqualByComparingTo("0.00");
        assertThat(orcamento.isZerado()).isTrue();
    }

    @Test
    void recusaAjusteQuePassaDaRendaEDizOExcesso() {
        orcamento.gerar(new RegraProporcional(), categorias);

        assertThatThrownBy(() -> orcamento.ajustarPrevisto(3, new BigDecimal("900.01")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Esse valor ultrapassa a renda do orçamento em R$ 0,01.");
        assertThat(orcamento.getReserva()).isEqualByComparingTo("600.00");
    }

    @Test
    void recusaAjusteDeCategoriaForaDoOrcamento() {
        orcamento.gerar(new RegraProporcional(), categorias);

        assertThatThrownBy(() -> orcamento.ajustarPrevisto(99, BigDecimal.ONE))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void recusaGerarSemCategorias() {
        assertThatThrownBy(() -> orcamento.gerar(new RegraBaseZero(), List.of()))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
