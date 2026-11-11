package com.vgrfinance.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.vgrfinance.exception.DadosInvalidosException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.ItemOrcamento;
import com.vgrfinance.model.Usuario;

/** Distribuições com resultado conhecido ao centavo e a propriedade "nunca passa da renda". */
class RegraProporcionalTest {

    private static final Usuario USUARIO = new Usuario("Teste", "teste@vgr.local", "hash");

    static Categoria categoria(String nome, boolean essencial) {
        return new Categoria(USUARIO, nome, essencial);
    }

    static Map<String, String> valores(List<ItemOrcamento> itens) {
        return itens.stream().collect(Collectors.toMap(
                i -> i.getCategoria().getNome(), i -> i.getValorPrevisto().toPlainString()));
    }

    static BigDecimal total(List<ItemOrcamento> itens) {
        return itens.stream().map(ItemOrcamento::getValorPrevisto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void distribui50_30_20ComRenda3000() {
        List<ItemOrcamento> itens = new RegraProporcional().distribuir(new BigDecimal("3000.00"), List.of(
                categoria("Moradia", true), categoria("Alimentação", true),
                categoria("Lazer", false), categoria("Delivery", false), categoria("Assinaturas", false)));

        assertThat(valores(itens)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "Alimentação", "750.00", "Moradia", "750.00",
                "Assinaturas", "300.00", "Delivery", "300.00", "Lazer", "300.00"));
        assertThat(total(itens)).isEqualByComparingTo("2400.00");
    }

    @Test
    void entregaOCentavoQueSobraEmOrdemAlfabetica() {
        List<ItemOrcamento> itens = new RegraProporcional().distribuir(new BigDecimal("1000.00"), List.of(
                categoria("Transporte", true), categoria("Moradia", true), categoria("Alimentação", true),
                categoria("Lazer", false), categoria("Delivery", false)));

        assertThat(valores(itens)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "Alimentação", "166.67", "Moradia", "166.67", "Transporte", "166.66",
                "Delivery", "150.00", "Lazer", "150.00"));
        assertThat(total(itens)).isEqualByComparingTo("800.00");
    }

    @Test
    void rendaComDizimasFechaNoCentavo() {
        List<ItemOrcamento> itens = new RegraProporcional().distribuir(new BigDecimal("3333.33"), List.of(
                categoria("A", true), categoria("B", true), categoria("C", true),
                categoria("D", false), categoria("E", false), categoria("F", false)));

        assertThat(valores(itens)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "A", "555.56", "B", "555.55", "C", "555.55",
                "D", "333.34", "E", "333.33", "F", "333.33"));
        assertThat(total(itens)).isEqualByComparingTo("2666.66");
    }

    @Test
    void aceitaPercentuaisProprios() {
        RegraProporcional regra = new RegraProporcional(
                new BigDecimal("60"), new BigDecimal("20"), new BigDecimal("20"));

        List<ItemOrcamento> itens = regra.distribuir(new BigDecimal("1000.00"),
                List.of(categoria("Moradia", true), categoria("Lazer", false)));

        assertThat(valores(itens)).containsEntry("Moradia", "600.00").containsEntry("Lazer", "200.00");
    }

    @Test
    void recusaPercentuaisQueNaoSomam100() {
        assertThatThrownBy(() -> new RegraProporcional(
                new BigDecimal("60"), new BigDecimal("30"), new BigDecimal("30")))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Os percentuais precisam somar 100.");
    }

    @Test
    void recusaPercentualNegativo() {
        assertThatThrownBy(() -> new RegraProporcional(
                new BigDecimal("110"), new BigDecimal("-30"), new BigDecimal("20")))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage("Os percentuais não podem ser negativos.");
    }

    @Test
    void grupoSemCategoriasDeixaAParteDeleNaReserva() {
        List<ItemOrcamento> itens = new RegraProporcional().distribuir(new BigDecimal("1000.00"),
                List.of(categoria("Moradia", true)));

        assertThat(total(itens)).isEqualByComparingTo("500.00");
    }

    @Test
    void nuncaPassaDaRendaEm20MilCasosAleatorios() {
        Random sorteio = new Random(42);
        for (int caso = 0; caso < 20_000; caso++) {
            BigDecimal renda = BigDecimal.valueOf(1 + sorteio.nextInt(2_000_000), 2);
            List<Categoria> categorias = new ArrayList<>();
            int quantidade = 1 + sorteio.nextInt(9);
            for (int i = 0; i < quantidade; i++) {
                categorias.add(categoria("C" + i, sorteio.nextBoolean()));
            }

            BigDecimal total = total(new RegraProporcional().distribuir(renda, categorias));

            assertThat(total).as("renda %s com %d categorias", renda, quantidade).isLessThanOrEqualTo(renda);
        }
    }
}
