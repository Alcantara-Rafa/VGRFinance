package com.vgrfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.vgrfinance.dto.AporteRequest;
import com.vgrfinance.dto.GastoPorCategoria;
import com.vgrfinance.dto.MetaRequest;
import com.vgrfinance.dto.MetaResponse;
import com.vgrfinance.exception.DadosInvalidosException;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.MetaAcumulacao;
import com.vgrfinance.model.MetaReducaoGasto;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.model.TipoMeta;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.MetaRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    private static final Integer USUARIO_ID = 1;

    // Novembro de 2026 em Brasília.
    private final Clock relogio = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Mock
    private MetaRepository metas;
    @Mock
    private CategoriaRepository categorias;
    @Mock
    private LancamentoRepository lancamentos;
    @Mock
    private UsuarioLogado usuarioLogado;

    private MetaService servico;
    private final Usuario usuario = new Usuario("Teste", "teste@vgr.local", "hash");
    private Categoria delivery;

    @BeforeEach
    void preparar() {
        servico = new MetaService(metas, categorias, lancamentos, usuarioLogado, relogio);
        delivery = new Categoria(usuario, "Delivery", false);
        ReflectionTestUtils.setField(delivery, "id", 5);
        lenient().when(usuarioLogado.id()).thenReturn(USUARIO_ID);
        lenient().when(usuarioLogado.obter()).thenReturn(usuario);
        lenient().when(metas.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private static MetaRequest pedido(TipoMeta tipo, String alvo, Integer categoriaId, String acumulado) {
        return new MetaRequest(tipo, " Meta ", new BigDecimal(alvo), categoriaId,
                acumulado == null ? null : new BigDecimal(acumulado));
    }

    @Test
    void criaMetaDeAcumulacaoComValorJaGuardado() {
        MetaResponse criada = servico.criar(pedido(TipoMeta.ACUMULACAO, "6000", null, "2400"));

        assertThat(criada.descricao()).isEqualTo("Meta");
        assertThat(criada.progresso()).isEqualByComparingTo("0.4000");
        assertThat(criada.faltante()).isEqualByComparingTo("3600.00");
        assertThat(criada.categoriaId()).isNull();
    }

    @Test
    void criaMetaDeReducaoApurandoOGastoDoMesAtual() {
        when(categorias.findByIdAndUsuarioId(5, USUARIO_ID)).thenReturn(Optional.of(delivery));
        when(lancamentos.somarPorCategoria(eq(USUARIO_ID), eq(TipoLancamento.DESPESA),
                eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 30))))
                .thenReturn(List.of(new GastoPorCategoria(5, "Delivery", false, new BigDecimal("310.00"))));

        MetaResponse criada = servico.criar(pedido(TipoMeta.REDUCAO_GASTO, "250", 5, null));

        assertThat(criada.mes()).isEqualTo("2026-11");
        assertThat(criada.situacao()).isEqualTo(MetaReducaoGasto.TETO_ULTRAPASSADO);
        assertThat(criada.excedente()).isEqualByComparingTo("60.00");
        assertThat(criada.valorAcumulado()).isNull();
    }

    @Test
    void reducaoExigeCategoria() {
        assertThatThrownBy(() -> servico.criar(pedido(TipoMeta.REDUCAO_GASTO, "250", null, null)))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(MetaService.MSG_REDUCAO_SEM_CATEGORIA);
        verify(metas, never()).save(any());
    }

    @Test
    void acumulacaoNaoTemCategoria() {
        assertThatThrownBy(() -> servico.criar(pedido(TipoMeta.ACUMULACAO, "6000", 5, null)))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(MetaService.MSG_ACUMULACAO_COM_CATEGORIA);
    }

    @Test
    void tipoNaoMudaDepoisDeCriada() {
        when(metas.findByIdAndUsuarioId(1, USUARIO_ID))
                .thenReturn(Optional.of(new MetaAcumulacao(usuario, "Reserva", new BigDecimal("6000.00"))));

        assertThatThrownBy(() -> servico.atualizar(1, pedido(TipoMeta.REDUCAO_GASTO, "250", 5, null)))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessage(MetaService.MSG_TIPO_IMUTAVEL);
    }

    @Test
    void aporteSoEmMetaDeAcumulacao() {
        when(metas.findByIdAndUsuarioId(2, USUARIO_ID)).thenReturn(Optional.of(
                new MetaReducaoGasto(usuario, "Delivery até 250", new BigDecimal("250.00"), delivery)));

        assertThatThrownBy(() -> servico.aportar(2, new AporteRequest(new BigDecimal("50"))))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage(MetaService.MSG_APORTE_EM_REDUCAO);
    }

    @Test
    void aporteSomaAoAcumulado() {
        when(metas.findByIdAndUsuarioId(1, USUARIO_ID))
                .thenReturn(Optional.of(new MetaAcumulacao(usuario, "Reserva", new BigDecimal("6000.00"))));

        MetaResponse depois = servico.aportar(1, new AporteRequest(new BigDecimal("600")));

        assertThat(depois.valorAcumulado()).isEqualByComparingTo("600.00");
        assertThat(depois.progresso()).isEqualByComparingTo("0.1000");
    }
}
