package com.vgrfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.vgrfinance.dto.OrcamentoRequest;
import com.vgrfinance.dto.OrcamentoResponse;
import com.vgrfinance.exception.DadosInvalidosException;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.TipoMetodo;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.OrcamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    private static final Integer USUARIO_ID = 1;
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    @Mock
    private OrcamentoRepository orcamentos;
    @Mock
    private CategoriaRepository categorias;
    @Mock
    private UsuarioLogado usuarioLogado;

    private OrcamentoService servico;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servico = new OrcamentoService(orcamentos, categorias, usuarioLogado, new MetodoOrcamentarioFactory());
        usuario = new Usuario("Teste", "teste@vgr.local", "hash");
        lenient().when(usuarioLogado.id()).thenReturn(USUARIO_ID);
        lenient().when(usuarioLogado.obter()).thenReturn(usuario);
    }

    private Categoria categoria(int id, String nome, boolean essencial) {
        Categoria categoria = new Categoria(usuario, nome, essencial);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    private static OrcamentoRequest pedido(TipoMetodo metodo, List<Integer> categoriaIds,
                                           String... percentuais) {
        BigDecimal[] pct = new BigDecimal[3];
        for (int i = 0; i < percentuais.length; i++) {
            pct[i] = new BigDecimal(percentuais[i]);
        }
        return new OrcamentoRequest("2026-11", new BigDecimal("3000"), metodo, pct[0], pct[1], pct[2], categoriaIds);
    }

    @Test
    void criaOrcamentoComRendaEmDuasCasas() {
        when(categorias.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(
                categoria(1, "Moradia", true), categoria(2, "Lazer", false)));

        OrcamentoResponse criado = servico.criar(pedido(TipoMetodo.PROPORCIONAL, null));

        assertThat(criado.rendaBase().toPlainString()).isEqualTo("3000.00");
        assertThat(criado.totalPrevisto()).isEqualByComparingTo("2400.00");
        assertThat(criado.reserva()).isEqualByComparingTo("600.00");
        assertThat(criado.avisos()).isEmpty();
        verify(orcamentos).save(any());
    }

    @Test
    void recusaSegundoOrcamentoNoMesmoMes() {
        when(orcamentos.existsByUsuarioIdAndMesReferencia(USUARIO_ID, NOVEMBRO)).thenReturn(true);

        assertThatThrownBy(() -> servico.criar(pedido(TipoMetodo.BASE_ZERO, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Já existe um orçamento para 2026-11");
        verify(orcamentos, never()).save(any());
    }

    @Test
    void pedeCategoriasQuandoNaoHaNenhuma() {
        when(categorias.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> servico.criar(pedido(TipoMetodo.BASE_ZERO, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage(OrcamentoService.MSG_SEM_CATEGORIAS);
    }

    @Test
    void recusaCategoriaEscolhidaQueNaoExiste() {
        when(categorias.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(categoria(1, "Moradia", true)));

        assertThatThrownBy(() -> servico.criar(pedido(TipoMetodo.BASE_ZERO, List.of(1, 99))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage(OrcamentoService.MSG_CATEGORIA_ESCOLHIDA_INEXISTENTE);
    }

    @Test
    void recusaPercentuaisIncompletos() {
        when(categorias.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(categoria(1, "Moradia", true)));

        assertThatThrownBy(() -> servico.criar(pedido(TipoMetodo.PROPORCIONAL, null, "60", "40")))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(MetodoOrcamentarioFactory.MSG_PERCENTUAIS_INCOMPLETOS);
    }

    @Test
    void avisaQuandoNaoHaCategoriaEssencial() {
        when(categorias.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(categoria(2, "Lazer", false)));

        OrcamentoResponse criado = servico.criar(pedido(TipoMetodo.PROPORCIONAL, null));

        assertThat(criado.avisos()).containsExactly(OrcamentoService.AVISO_SEM_ESSENCIAIS);
        assertThat(criado.reserva()).isEqualByComparingTo("2100.00");
    }
}
