package com.vgrfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.vgrfinance.dominio.RegraBaseZero;
import com.vgrfinance.dto.AcompanhamentoResponse;
import com.vgrfinance.dto.GastoPorCategoria;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.SituacaoItem;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.OrcamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

/** Exemplo de novembro de 2026: renda 3.000,00 com Delivery, Alimentação e Lazer no orçamento. */
@ExtendWith(MockitoExtension.class)
class AcompanhamentoServiceTest {

    private static final Integer USUARIO_ID = 1;
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);
    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 11, 30);

    @Mock
    private OrcamentoRepository orcamentos;
    @Mock
    private LancamentoRepository lancamentos;
    @Mock
    private UsuarioLogado usuarioLogado;

    private AcompanhamentoService servico;
    private final Usuario usuario = new Usuario("Teste", "teste@vgr.local", "hash");

    private Categoria categoria(int id, String nome, boolean essencial) {
        Categoria categoria = new Categoria(usuario, nome, essencial);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    private static GastoPorCategoria gasto(int id, String nome, String total) {
        return new GastoPorCategoria(id, nome, false, new BigDecimal(total));
    }

    @BeforeEach
    void preparar() {
        servico = new AcompanhamentoService(orcamentos, lancamentos, usuarioLogado);
        lenient().when(usuarioLogado.id()).thenReturn(USUARIO_ID);

        Orcamento orcamento = new Orcamento(usuario, NOVEMBRO, new BigDecimal("3000.00"));
        orcamento.gerar(new RegraBaseZero(), List.of(
                categoria(1, "Delivery", false), categoria(2, "Alimentação", true), categoria(3, "Lazer", false)));
        orcamento.ajustarPrevisto(1, new BigDecimal("300.00"));
        orcamento.ajustarPrevisto(2, new BigDecimal("750.00"));
        orcamento.ajustarPrevisto(3, new BigDecimal("300.00"));
        lenient().when(orcamentos.findByUsuarioIdAndMesReferencia(USUARIO_ID, NOVEMBRO))
                .thenReturn(Optional.of(orcamento));
    }

    @Test
    void comparaPrevistoComRealizadoEMostraOForaDoPlano() {
        when(lancamentos.somarPorCategoria(USUARIO_ID, TipoLancamento.DESPESA, INICIO, FIM)).thenReturn(List.of(
                gasto(1, "Delivery", "412.00"), gasto(2, "Alimentação", "600.00"),
                gasto(3, "Lazer", "150.00"), gasto(4, "Farmácia", "90.00")));

        AcompanhamentoResponse resposta = servico.consultarPorMes("2026-11");

        assertThat(resposta.itens()).extracting(AcompanhamentoResponse.Item::categoriaNome, AcompanhamentoResponse.Item::situacao)
                .containsExactly(
                        tuple("Alimentação", SituacaoItem.ATENCAO),
                        tuple("Delivery", SituacaoItem.ESTOURADO),
                        tuple("Lazer", SituacaoItem.NO_PLANO));
        assertThat(resposta.foraDoPlano()).singleElement()
                .satisfies(f -> assertThat(f.categoriaNome()).isEqualTo("Farmácia"));
        assertThat(resposta.totalRealizado()).isEqualByComparingTo("1252.00");
        assertThat(resposta.restanteDaRenda()).isEqualByComparingTo("1748.00");
        assertThat(resposta.totalExcedentes()).isEqualByComparingTo("202.00");
        assertThat(resposta.itensEstourados()).isEqualTo(1);
    }

    @Test
    void itemSemGastoFicaComRealizadoZero() {
        when(lancamentos.somarPorCategoria(USUARIO_ID, TipoLancamento.DESPESA, INICIO, FIM)).thenReturn(List.of());

        AcompanhamentoResponse resposta = servico.consultarPorMes("2026-11");

        assertThat(resposta.itens()).allSatisfy(item -> {
            assertThat(item.realizado()).isEqualByComparingTo("0");
            assertThat(item.situacao()).isEqualTo(SituacaoItem.NO_PLANO);
        });
        assertThat(resposta.foraDoPlano()).isEmpty();
        assertThat(resposta.totalRealizado()).isEqualByComparingTo("0");
    }

    @Test
    void pedeParaCriarOrcamentoQuandoOMesNaoTem() {
        when(orcamentos.findByUsuarioIdAndMesReferencia(USUARIO_ID, YearMonth.of(2026, 12))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.consultarPorMes("2026-12"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage(AcompanhamentoService.MSG_SEM_ORCAMENTO);
    }
}
