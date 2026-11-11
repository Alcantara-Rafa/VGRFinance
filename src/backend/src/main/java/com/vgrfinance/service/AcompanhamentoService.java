package com.vgrfinance.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.dto.AcompanhamentoResponse;
import com.vgrfinance.dto.GastoPorCategoria;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.ItemOrcamento;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.OrcamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

/** RF05: compara o orçamento do mês com as despesas lançadas no mesmo mês. */
@Service
public class AcompanhamentoService {

    static final String MSG_SEM_ORCAMENTO = "Não há orçamento para este mês. Crie um para acompanhar os gastos.";

    private final OrcamentoRepository orcamentos;
    private final LancamentoRepository lancamentos;
    private final UsuarioLogado usuarioLogado;

    public AcompanhamentoService(OrcamentoRepository orcamentos, LancamentoRepository lancamentos,
                                 UsuarioLogado usuarioLogado) {
        this.orcamentos = orcamentos;
        this.lancamentos = lancamentos;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public AcompanhamentoResponse consultarPorMes(String mes) {
        Orcamento orcamento = orcamentos.findByUsuarioIdAndMesReferencia(usuarioLogado.id(), YearMonth.parse(mes))
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_SEM_ORCAMENTO));
        return montar(orcamento);
    }

    /** Também usado pelo diagnóstico, que parte do mesmo previsto × realizado. */
    AcompanhamentoResponse montar(Orcamento orcamento) {
        YearMonth mes = orcamento.getMesReferencia();
        List<GastoPorCategoria> gastos = lancamentos.somarPorCategoria(
                usuarioLogado.id(), TipoLancamento.DESPESA, mes.atDay(1), mes.atEndOfMonth());
        Map<Integer, BigDecimal> totalPorCategoria = gastos.stream()
                .collect(Collectors.toMap(GastoPorCategoria::categoriaId, GastoPorCategoria::total));

        // Cada item recebe o que foi gasto na sua categoria; sem gasto, zero.
        orcamento.getItens().forEach(item -> item.informarRealizado(
                totalPorCategoria.getOrDefault(item.getCategoria().getId(), BigDecimal.ZERO)));

        // RN31: despesa em categoria que não está no orçamento.
        Set<Integer> categoriasDoOrcamento = orcamento.getItens().stream()
                .map(item -> item.getCategoria().getId())
                .collect(Collectors.toSet());
        List<AcompanhamentoResponse.ForaDoPlano> foraDoPlano = gastos.stream()
                .filter(g -> !categoriasDoOrcamento.contains(g.categoriaId()))
                .sorted(Comparator.comparing(GastoPorCategoria::categoriaNome, String.CASE_INSENSITIVE_ORDER))
                .map(g -> new AcompanhamentoResponse.ForaDoPlano(g.categoriaId(), g.categoriaNome(),
                        g.essencial(), g.total()))
                .toList();

        BigDecimal realizadoFora = somar(foraDoPlano.stream().map(AcompanhamentoResponse.ForaDoPlano::realizado).toList());
        BigDecimal totalRealizado = somar(orcamento.getItens().stream().map(ItemOrcamento::getValorRealizado).toList())
                .add(realizadoFora);
        BigDecimal excedentes = somar(orcamento.getItens().stream().map(ItemOrcamento::getExcedente).toList())
                .add(realizadoFora);

        List<AcompanhamentoResponse.Item> itens = orcamento.getItens().stream()
                .sorted(Comparator.comparing(i -> i.getCategoria().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(AcompanhamentoService::paraResposta)
                .toList();

        return new AcompanhamentoResponse(orcamento.getId(), mes.toString(), orcamento.getRendaBase(),
                orcamento.getTotalPrevisto(), totalRealizado, orcamento.getRendaBase().subtract(totalRealizado),
                excedentes, (int) orcamento.getItens().stream().filter(ItemOrcamento::isEstourado).count(),
                itens, foraDoPlano);
    }

    private static BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static AcompanhamentoResponse.Item paraResposta(ItemOrcamento item) {
        Categoria c = item.getCategoria();
        return new AcompanhamentoResponse.Item(c.getId(), c.getNome(), c.isEssencial(),
                item.getValorPrevisto(), item.getValorRealizado(), item.getDesvio(), item.getExcedente(),
                item.getPercentualUtilizado(), item.getSituacao());
    }
}
