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
import com.vgrfinance.dto.GastoPorCategoria;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Lancamento;
import com.vgrfinance.model.Meta;
import com.vgrfinance.model.MetaAcumulacao;
import com.vgrfinance.model.MetaReducaoGasto;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.LancamentoRepository;

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
    @Autowired
    private LancamentoRepository lancamentos;

    private Usuario usuario;
    private Categoria mercado;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("Persistência", "persistencia@vgr.local", "hash");
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

    @Test
    void somaSoDespesasDoMesIncluindoPrimeiroEUltimoDia() {
        em.persist(new Lancamento(usuario, mercado, TipoLancamento.DESPESA,
                new BigDecimal("10.00"), LocalDate.of(2026, 11, 1), null));
        em.persist(new Lancamento(usuario, mercado, TipoLancamento.DESPESA,
                new BigDecimal("20.00"), LocalDate.of(2026, 11, 30), null));
        em.persist(new Lancamento(usuario, mercado, TipoLancamento.DESPESA,
                new BigDecimal("50.00"), LocalDate.of(2026, 10, 31), null));
        em.persist(new Lancamento(usuario, mercado, TipoLancamento.RECEITA,
                new BigDecimal("3200.00"), LocalDate.of(2026, 11, 5), null));
        em.flush();

        List<GastoPorCategoria> gastos = lancamentos.somarPorCategoria(usuario.getId(), TipoLancamento.DESPESA,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(gastos).singleElement().satisfies(g -> {
            assertThat(g.categoriaNome()).isEqualTo("Mercado");
            assertThat(g.total()).isEqualByComparingTo("30.00");
        });
    }

    @Test
    void metasVoltamDoBancoComoASuaFilha() {
        MetaAcumulacao reserva = new MetaAcumulacao(usuario, "Reserva", new BigDecimal("6000.00"));
        reserva.registrarAporte(new BigDecimal("2400.00"));
        MetaReducaoGasto teto = new MetaReducaoGasto(usuario, "Mercado até 800", new BigDecimal("800.00"), mercado);
        em.persist(reserva);
        em.persist(teto);
        em.flush();
        em.clear();

        Meta relidaReserva = em.find(Meta.class, reserva.getId());
        Meta relidoTeto = em.find(Meta.class, teto.getId());

        assertThat(relidaReserva).isInstanceOf(MetaAcumulacao.class);
        assertThat(((MetaAcumulacao) relidaReserva).getValorAcumulado()).isEqualByComparingTo("2400.00");
        assertThat(relidoTeto).isInstanceOf(MetaReducaoGasto.class);
        assertThat(((MetaReducaoGasto) relidoTeto).getCategoria().getNome()).isEqualTo("Mercado");
    }

    @Test
    void bancoRecusaMetaDeAcumulacaoComCategoria() {
        assertThatThrownBy(() -> em.createNativeQuery("""
                insert into metas (usuario_id, tipo_meta, descricao, valor_alvo, valor_acumulado, categoria_id)
                values (?1, 'ACUMULACAO', 'Inválida', 100, 0, ?2)
                """)
                .setParameter(1, usuario.getId())
                .setParameter(2, mercado.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    /** Com id gerado pelo banco, o INSERT pode sair já no persist; o flush garante que saiu. */
    private void gravar(Object entidade) {
        em.persist(entidade);
        em.flush();
    }
}
