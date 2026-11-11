package com.vgrfinance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.vgrfinance.dominio.RegraBaseZero;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Lancamento;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.Papel;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.model.Usuario;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

/**
 * Prova, no PostgreSQL de verdade, que classes e tabelas batem e que o banco
 * recusa dado ruim mesmo se o Java deixasse passar. Cada teste é desfeito no fim.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenciaTest {

    @Autowired
    private EntityManager em;

    private Usuario usuario;
    private Categoria mercado;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("Persistência", "persistencia@vgr.local", "hash", Papel.USUARIO);
        mercado = new Categoria(usuario, "Mercado", true);
        em.persist(usuario);
        em.persist(mercado);
    }

    @Test
    void guardaCentavosSemPerda() {
        Lancamento lancamento = new Lancamento(usuario, mercado, TipoLancamento.DESPESA,
                new BigDecimal("1234.56"), LocalDate.of(2026, 11, 5), "Compra do mês");
        em.persist(lancamento);
        em.flush();
        em.clear();

        Lancamento relido = em.find(Lancamento.class, lancamento.getId());

        assertThat(relido.getValor()).isEqualByComparingTo("1234.56");
        assertThat(relido.getCategoria().getNome()).isEqualTo("Mercado");
    }

    @Test
    void bancoRecusaValorNegativo() {
        Lancamento negativo = new Lancamento(usuario, mercado, TipoLancamento.DESPESA,
                new BigDecimal("-1.00"), LocalDate.of(2026, 11, 5), null);

        assertThatThrownBy(() -> gravar(negativo)).isInstanceOf(PersistenceException.class);
    }

    @Test
    void bancoRecusaNomeDeCategoriaRepetidoEmOutraCaixa() {
        Categoria repetida = new Categoria(usuario, "MERCADO", false);

        assertThatThrownBy(() -> gravar(repetida)).isInstanceOf(PersistenceException.class);
    }

    @Test
    void bancoRecusaDespesaSemCategoria() {
        Lancamento semCategoria = new Lancamento(usuario, null, TipoLancamento.DESPESA,
                new BigDecimal("10.00"), LocalDate.of(2026, 11, 5), null);

        assertThatThrownBy(() -> gravar(semCategoria)).isInstanceOf(PersistenceException.class);
    }

    @Test
    void orcamentoGravaItensEExcluiEmCascata() {
        Orcamento orcamento = new Orcamento(usuario, YearMonth.of(2026, 11), new BigDecimal("1000.00"));
        orcamento.gerar(new RegraBaseZero(), List.of(mercado));
        em.persist(orcamento);
        em.flush();
        em.clear();

        Orcamento relido = em.find(Orcamento.class, orcamento.getId());
        assertThat(relido.getMesReferencia()).isEqualTo(YearMonth.of(2026, 11));
        assertThat(relido.getItens()).singleElement()
                .satisfies(item -> assertThat(item.getValorPrevisto()).isEqualByComparingTo("1000.00"));

        em.remove(relido);
        em.flush();
        Long itensRestantes = em.createQuery(
                "select count(i) from ItemOrcamento i where i.categoria.id = :id", Long.class)
                .setParameter("id", mercado.getId())
                .getSingleResult();
        assertThat(itensRestantes).isZero();
        assertThat(em.find(Categoria.class, mercado.getId())).isNotNull();
    }

    @Test
    void bancoRecusaDoisOrcamentosNoMesmoMes() {
        Orcamento primeiro = new Orcamento(usuario, YearMonth.of(2026, 12), new BigDecimal("1000.00"));
        primeiro.gerar(new RegraBaseZero(), List.of(mercado));
        gravar(primeiro);
        Orcamento segundo = new Orcamento(usuario, YearMonth.of(2026, 12), new BigDecimal("2000.00"));
        segundo.gerar(new RegraBaseZero(), List.of(mercado));

        assertThatThrownBy(() -> gravar(segundo)).isInstanceOf(PersistenceException.class);
    }

    /** Com id gerado pelo banco, o INSERT pode sair já no persist; o flush garante que saiu. */
    private void gravar(Object entidade) {
        em.persist(entidade);
        em.flush();
    }
}
