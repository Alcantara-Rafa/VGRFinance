package com.vgrfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.vgrfinance.dto.LancamentoRequest;
import com.vgrfinance.dto.LancamentoResponse;
import com.vgrfinance.exception.DadosInvalidosException;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Lancamento;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    private static final Integer USUARIO_ID = 1;

    // CA8: 05/10 às 01:30 em UTC ainda é 04/10 em Brasília.
    private final Clock relogio = Clock.fixed(Instant.parse("2026-10-05T01:30:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Mock
    private LancamentoRepository lancamentos;
    @Mock
    private CategoriaRepository categorias;
    @Mock
    private UsuarioLogado usuarioLogado;

    private LancamentoService servico;
    private Usuario usuario;
    private Categoria mercado;

    @BeforeEach
    void preparar() {
        servico = new LancamentoService(lancamentos, categorias, usuarioLogado, relogio);
        usuario = new Usuario("Teste", "teste@vgr.local", "hash");
        mercado = new Categoria(usuario, "Mercado", true);
        ReflectionTestUtils.setField(mercado, "id", 1);
        lenient().when(usuarioLogado.id()).thenReturn(USUARIO_ID);
        lenient().when(usuarioLogado.obter()).thenReturn(usuario);
        lenient().when(lancamentos.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    void lancamentoRapidoViraDespesaDeHojeEmBrasiliaComDuasCasas() {
        when(categorias.findByIdAndUsuarioId(1, USUARIO_ID)).thenReturn(Optional.of(mercado));

        LancamentoResponse criado = servico.criar(new LancamentoRequest(new BigDecimal("10"), 1, null, null, "  "));

        assertThat(criado.tipo()).isEqualTo(TipoLancamento.DESPESA);
        assertThat(criado.data()).isEqualTo(LocalDate.of(2026, 10, 4));
        assertThat(criado.descricao()).isNull();
        assertThat(criado.valor().toPlainString()).isEqualTo("10.00");
        assertThat(criado.categoriaNome()).isEqualTo("Mercado");
    }

    @Test
    void mantemTipoDataEDescricaoInformados() {
        when(categorias.findByIdAndUsuarioId(1, USUARIO_ID)).thenReturn(Optional.of(mercado));
        LocalDate dia = LocalDate.of(2026, 9, 30);

        LancamentoResponse criado = servico.criar(
                new LancamentoRequest(new BigDecimal("25.90"), 1, TipoLancamento.DESPESA, dia, " Feira "));

        assertThat(criado.data()).isEqualTo(dia);
        assertThat(criado.descricao()).isEqualTo("Feira");
    }

    @Test
    void aceitaReceitaSemCategoria() {
        LancamentoResponse criado = servico.criar(
                new LancamentoRequest(new BigDecimal("3200.00"), null, TipoLancamento.RECEITA, null, "Salário"));

        assertThat(criado.tipo()).isEqualTo(TipoLancamento.RECEITA);
        assertThat(criado.categoriaId()).isNull();
    }

    @Test
    void recusaDespesaSemCategoria() {
        LancamentoRequest semCategoria = new LancamentoRequest(new BigDecimal("10"), null, null, null, null);

        assertThatThrownBy(() -> servico.criar(semCategoria))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(LancamentoService.MSG_DESPESA_SEM_CATEGORIA);
        verify(lancamentos, never()).save(any());
    }

    @Test
    void recusaCategoriaInexistenteENaoSalva() {
        when(categorias.findByIdAndUsuarioId(99, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.criar(new LancamentoRequest(new BigDecimal("10"), 99, null, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage(CategoriaService.MSG_NAO_ENCONTRADA);
        verify(lancamentos, never()).save(any());
    }

    @Test
    void atualizarAlteraOsDados() {
        Lancamento existente = new Lancamento(usuario, mercado, TipoLancamento.DESPESA,
                new BigDecimal("10.00"), LocalDate.of(2026, 10, 1), null);
        when(lancamentos.findByIdAndUsuarioId(5, USUARIO_ID)).thenReturn(Optional.of(existente));
        when(categorias.findByIdAndUsuarioId(1, USUARIO_ID)).thenReturn(Optional.of(mercado));

        LancamentoResponse alterado = servico.atualizar(5,
                new LancamentoRequest(new BigDecimal("42.5"), 1, null, LocalDate.of(2026, 10, 2), "Almoço"));

        assertThat(alterado.valor().toPlainString()).isEqualTo("42.50");
        assertThat(alterado.data()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(alterado.descricao()).isEqualTo("Almoço");
    }

    @Test
    void filtraPeloMesDoPrimeiroAoUltimoDia() {
        Lancamento fevereiro = new Lancamento(usuario, mercado, TipoLancamento.DESPESA,
                new BigDecimal("10.00"), LocalDate.of(2028, 2, 29), null);
        when(lancamentos.findByUsuarioIdAndDataBetweenOrderByDataDescIdDesc(
                USUARIO_ID, LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29))).thenReturn(List.of(fevereiro));

        List<LancamentoResponse> doMes = servico.listar("2028-02");

        assertThat(doMes).singleElement().satisfies(l -> assertThat(l.data()).isEqualTo(LocalDate.of(2028, 2, 29)));
    }

    @Test
    void excluirLancamentoInexistenteResponde404() {
        when(lancamentos.findByIdAndUsuarioId(7, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.excluir(7))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage(LancamentoService.MSG_NAO_ENCONTRADO);
    }
}
