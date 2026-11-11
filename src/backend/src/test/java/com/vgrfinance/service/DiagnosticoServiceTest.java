package com.vgrfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.vgrfinance.dominio.FaixaDiagnostica;
import com.vgrfinance.dto.AcompanhamentoResponse;
import com.vgrfinance.dto.DiagnosticoResponse;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.SituacaoItem;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.model.Usuario;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.OrcamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@ExtendWith(MockitoExtension.class)
class DiagnosticoServiceTest {

    private static final Integer USUARIO_ID = 1;
    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    @Mock
    private OrcamentoRepository orcamentos;
    @Mock
    private LancamentoRepository lancamentos;
    @Mock
    private AcompanhamentoService acompanhamento;
    @Mock
    private UsuarioLogado usuarioLogado;

    private DiagnosticoService servico;
    private final Orcamento orcamento = new Orcamento(new Usuario("Teste", "teste@vgr.local", "hash"),
            NOVEMBRO, new BigDecimal("3000.00"));

    @BeforeEach
    void preparar() {
        servico = new DiagnosticoService(orcamentos, lancamentos, acompanhamento, usuarioLogado);
        lenient().when(usuarioLogado.id()).thenReturn(USUARIO_ID);
    }

    private void temDespesaNoMes(boolean tem) {
        when(lancamentos.existsByUsuarioIdAndTipoAndDataBetween(USUARIO_ID, TipoLancamento.DESPESA,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(tem);
    }

    private static AcompanhamentoResponse.Item item(boolean essencial, String realizado) {
        BigDecimal valor = new BigDecimal(realizado);
        return new AcompanhamentoResponse.Item(1, "Categoria", essencial, valor, valor, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ONE, SituacaoItem.ATENCAO);
    }

    @Test
    void pedeOrcamentoQuandoOMesNaoTem() {
        when(orcamentos.findByUsuarioIdAndMesReferencia(USUARIO_ID, NOVEMBRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.gerar("2026-11"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage(DiagnosticoService.MSG_SEM_ORCAMENTO);
    }

    @Test
    void pedeUmGastoQuandoOMesNaoTemDespesa() {
        when(orcamentos.findByUsuarioIdAndMesReferencia(USUARIO_ID, NOVEMBRO)).thenReturn(Optional.of(orcamento));
        temDespesaNoMes(false);

        assertThatThrownBy(() -> servico.gerar("2026-11"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage(DiagnosticoService.MSG_SEM_DESPESA);
    }

    @Test
    void somaEssenciaisDoOrcamentoEDeForaDoPlano() {
        when(orcamentos.findByUsuarioIdAndMesReferencia(USUARIO_ID, NOVEMBRO)).thenReturn(Optional.of(orcamento));
        temDespesaNoMes(true);
        when(acompanhamento.montar(orcamento)).thenReturn(new AcompanhamentoResponse(null, "2026-11",
                new BigDecimal("3000.00"), new BigDecimal("2400.00"), new BigDecimal("2550.00"),
                new BigDecimal("450.00"), new BigDecimal("112.00"), 1,
                List.of(item(true, "1200.00"), item(false, "1200.00")),
                List.of(new AcompanhamentoResponse.ForaDoPlano(9, "Farmácia", true, new BigDecimal("150.00")))));

        DiagnosticoResponse resposta = servico.gerar("2026-11");

        assertThat(resposta.sobra()).isEqualByComparingTo("450.00");
        assertThat(resposta.indicadores()).extracting(i -> i.codigo() + " " + i.faixa())
                .containsExactly("TAXA_POUPANCA " + FaixaDiagnostica.ATENCAO,
                        "ADERENCIA_ORCAMENTO " + FaixaDiagnostica.SAUDAVEL,
                        "PESO_ESSENCIAIS " + FaixaDiagnostica.SAUDAVEL);
        // 1.200,00 do item essencial + 150,00 fora do plano = 1.350,00, ou 45% da renda.
        assertThat(resposta.indicadores().get(2).valor()).isEqualByComparingTo("0.4500");
    }
}
