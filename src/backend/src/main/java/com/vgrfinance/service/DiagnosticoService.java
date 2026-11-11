package com.vgrfinance.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.dominio.Diagnostico;
import com.vgrfinance.dto.AcompanhamentoResponse;
import com.vgrfinance.dto.DiagnosticoResponse;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.OrcamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

/** RF07: diagnóstico educativo do mês, calculado na hora a partir do acompanhamento. */
@Service
public class DiagnosticoService {

    static final String MSG_SEM_ORCAMENTO = "Para gerar o diagnóstico, crie primeiro o orçamento deste mês.";
    static final String MSG_SEM_DESPESA = "Para gerar o diagnóstico, registre ao menos um gasto neste mês.";

    private final OrcamentoRepository orcamentos;
    private final LancamentoRepository lancamentos;
    private final AcompanhamentoService acompanhamento;
    private final UsuarioLogado usuarioLogado;

    public DiagnosticoService(OrcamentoRepository orcamentos, LancamentoRepository lancamentos,
                              AcompanhamentoService acompanhamento, UsuarioLogado usuarioLogado) {
        this.orcamentos = orcamentos;
        this.lancamentos = lancamentos;
        this.acompanhamento = acompanhamento;
        this.usuarioLogado = usuarioLogado;
    }

    /** RN16: exige orçamento do mês e ao menos uma despesa no mês. */
    @Transactional(readOnly = true)
    public DiagnosticoResponse gerar(String mes) {
        YearMonth periodo = YearMonth.parse(mes);
        Orcamento orcamento = orcamentos.findByUsuarioIdAndMesReferencia(usuarioLogado.id(), periodo)
                .orElseThrow(() -> new RegraDeNegocioException(MSG_SEM_ORCAMENTO));
        if (!lancamentos.existsByUsuarioIdAndTipoAndDataBetween(usuarioLogado.id(), TipoLancamento.DESPESA,
                periodo.atDay(1), periodo.atEndOfMonth())) {
            throw new RegraDeNegocioException(MSG_SEM_DESPESA);
        }

        AcompanhamentoResponse mesAcompanhado = acompanhamento.montar(orcamento);
        // D10: a renda líquida do diagnóstico é a renda base do orçamento do mês.
        Diagnostico diagnostico = new Diagnostico(orcamento.getRendaBase(), mesAcompanhado.totalRealizado(),
                despesaEssenciais(mesAcompanhado), mesAcompanhado.totalPrevisto(), mesAcompanhado.totalExcedentes());

        return new DiagnosticoResponse(periodo.toString(), diagnostico.getRendaLiquida(),
                diagnostico.getDespesaTotal(), diagnostico.getSobra(),
                List.of(TextosDiagnostico.taxaPoupanca(diagnostico),
                        TextosDiagnostico.aderencia(diagnostico, mesAcompanhado.totalExcedentes()),
                        TextosDiagnostico.pesoEssenciais(diagnostico)));
    }

    /** Gasto em categorias essenciais, estejam elas no orçamento ou fora do plano. */
    private static BigDecimal despesaEssenciais(AcompanhamentoResponse mes) {
        Stream<BigDecimal> dosItens = mes.itens().stream()
                .filter(AcompanhamentoResponse.Item::essencial)
                .map(AcompanhamentoResponse.Item::realizado);
        Stream<BigDecimal> deFora = mes.foraDoPlano().stream()
                .filter(AcompanhamentoResponse.ForaDoPlano::essencial)
                .map(AcompanhamentoResponse.ForaDoPlano::realizado);
        return Stream.concat(dosItens, deFora).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
