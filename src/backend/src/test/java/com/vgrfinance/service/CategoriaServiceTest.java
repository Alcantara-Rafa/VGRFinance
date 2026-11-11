package com.vgrfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vgrfinance.dto.CategoriaRequest;
import com.vgrfinance.dto.CategoriaResponse;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.ItemOrcamentoRepository;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final Integer USUARIO_ID = 1;

    @Mock
    private CategoriaRepository categorias;
    @Mock
    private LancamentoRepository lancamentos;
    @Mock
    private ItemOrcamentoRepository itensOrcamento;
    @Mock
    private UsuarioLogado usuarioLogado;

    private CategoriaService servico;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servico = new CategoriaService(categorias, lancamentos, itensOrcamento, usuarioLogado);
        usuario = new Usuario("Teste", "teste@vgr.local", "hash");
        lenient().when(usuarioLogado.id()).thenReturn(USUARIO_ID);
        lenient().when(usuarioLogado.obter()).thenReturn(usuario);
        lenient().when(categorias.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    void criaCategoriaSemEspacosNasPontas() {
        CategoriaResponse criada = servico.criar(new CategoriaRequest("  Moradia ", true));

        assertThat(criada.nome()).isEqualTo("Moradia");
        assertThat(criada.essencial()).isTrue();
    }

    @Test
    void recusaNomeRepetidoMesmoComOutraCaixa() {
        when(categorias.existsByUsuarioIdAndNomeIgnoreCase(USUARIO_ID, "moradia")).thenReturn(true);

        assertThatThrownBy(() -> servico.criar(new CategoriaRequest("moradia", true)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage(CategoriaService.MSG_NOME_REPETIDO);
        verify(categorias, never()).save(any());
    }

    @Test
    void editarSemTrocarONomeNaoConflitaConsigo() {
        Categoria moradia = new Categoria(usuario, "Moradia", false);
        when(categorias.findByIdAndUsuarioId(3, USUARIO_ID)).thenReturn(Optional.of(moradia));
        when(categorias.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(USUARIO_ID, "Moradia", 3)).thenReturn(false);

        CategoriaResponse alterada = servico.atualizar(3, new CategoriaRequest("Moradia", true));

        assertThat(alterada.essencial()).isTrue();
    }

    @Test
    void recusaExcluirCategoriaComLancamentos() {
        Categoria mercado = new Categoria(usuario, "Mercado", true);
        when(categorias.findByIdAndUsuarioId(2, USUARIO_ID)).thenReturn(Optional.of(mercado));
        when(lancamentos.existsByCategoriaId(2)).thenReturn(true);

        assertThatThrownBy(() -> servico.excluir(2))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage(CategoriaService.MSG_EM_USO);
        verify(categorias, never()).delete(any());
    }

    @Test
    void recusaExcluirCategoriaUsadaEmOrcamento() {
        Categoria moradia = new Categoria(usuario, "Moradia", true);
        when(categorias.findByIdAndUsuarioId(5, USUARIO_ID)).thenReturn(Optional.of(moradia));
        when(itensOrcamento.existsByCategoriaId(5)).thenReturn(true);

        assertThatThrownBy(() -> servico.excluir(5))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage(CategoriaService.MSG_EM_USO);
        verify(categorias, never()).delete(any());
    }

    @Test
    void excluiCategoriaSemLancamentosNemOrcamento() {
        Categoria lazer = new Categoria(usuario, "Lazer", false);
        when(categorias.findByIdAndUsuarioId(4, USUARIO_ID)).thenReturn(Optional.of(lazer));

        servico.excluir(4);

        verify(categorias).delete(lazer);
    }
}
