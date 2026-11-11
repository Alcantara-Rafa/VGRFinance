package com.vgrfinance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.dto.AporteRequest;
import com.vgrfinance.dto.GastoPorCategoria;
import com.vgrfinance.dto.MetaRequest;
import com.vgrfinance.dto.MetaResponse;
import com.vgrfinance.exception.DadosInvalidosException;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Meta;
import com.vgrfinance.model.MetaAcumulacao;
import com.vgrfinance.model.MetaReducaoGasto;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.MetaRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

/**
 * RF06: metas de acumulação e de redução de gasto.
 * O serviço só conhece a classe abstrata Meta: o que muda de uma filha para outra
 * (aporte, campos próprios, resposta) é resolvido por polimorfismo, sem instanceof.
 */
@Service
public class MetaService {

    static final String MSG_NAO_ENCONTRADA = "Meta não encontrada.";
    static final String MSG_TIPO_IMUTAVEL = "O tipo de uma meta não pode ser alterado. Crie uma nova meta.";
    static final String MSG_REDUCAO_SEM_CATEGORIA = "Escolha a categoria da meta de redução de gasto.";

    private final MetaRepository metas;
    private final CategoriaRepository categorias;
    private final LancamentoRepository lancamentos;
    private final UsuarioLogado usuarioLogado;
    private final Clock relogio;

    public MetaService(MetaRepository metas, CategoriaRepository categorias, LancamentoRepository lancamentos,
                       UsuarioLogado usuarioLogado, Clock relogio) {
        this.metas = metas;
        this.categorias = categorias;
        this.lancamentos = lancamentos;
        this.usuarioLogado = usuarioLogado;
        this.relogio = relogio;
    }

    /** Sem mês, vale o mês atual em Brasília (usado na apuração das metas de redução). */
    @Transactional(readOnly = true)
    public List<MetaResponse> listar(String mes) {
        YearMonth periodo = periodoDe(mes);
        return metas.findByUsuarioIdOrderByIdAsc(usuarioLogado.id()).stream()
                .map(meta -> paraResposta(meta, periodo))
                .toList();
    }

    @Transactional(readOnly = true)
    public MetaResponse buscar(Integer id, String mes) {
        return paraResposta(buscarEntidade(id), periodoDe(mes));
    }

    /** Único ponto que escolhe a filha: depois de criada, a meta é tratada só como Meta. */
    @Transactional
    public MetaResponse criar(MetaRequest requisicao) {
        String descricao = requisicao.descricao().strip();
        BigDecimal valorAlvo = dinheiro(requisicao.valorAlvo());
        Meta meta = switch (requisicao.tipo()) {
            case ACUMULACAO -> new MetaAcumulacao(usuarioLogado.obter(), descricao, valorAlvo);
            case REDUCAO_GASTO -> new MetaReducaoGasto(usuarioLogado.obter(), descricao, valorAlvo, null);
        };
        definirDetalhes(meta, requisicao);
        return paraResposta(metas.save(meta), periodoDe(null));
    }

    /** RN33: o tipo não muda depois de criada. */
    @Transactional
    public MetaResponse atualizar(Integer id, MetaRequest requisicao) {
        Meta meta = buscarEntidade(id);
        if (meta.getTipo() != requisicao.tipo()) {
            throw new DadosInvalidosException(MSG_TIPO_IMUTAVEL);
        }
        meta.atualizarBase(requisicao.descricao().strip(), dinheiro(requisicao.valorAlvo()));
        definirDetalhes(meta, requisicao);
        return paraResposta(meta, periodoDe(null));
    }

    /** RN32: cada tipo de meta decide se aceita aporte (a de redução recusa). */
    @Transactional
    public MetaResponse aportar(Integer id, AporteRequest requisicao) {
        Meta meta = buscarEntidade(id);
        meta.registrarAporte(dinheiro(requisicao.valor()));
        return paraResposta(meta, periodoDe(null));
    }

    @Transactional
    public void excluir(Integer id) {
        metas.delete(buscarEntidade(id));
    }

    /** A filha valida e grava o que é dela (RN34). */
    private void definirDetalhes(Meta meta, MetaRequest requisicao) {
        BigDecimal acumulado = requisicao.valorAcumulado() == null ? null : dinheiro(requisicao.valorAcumulado());
        meta.definirDetalhes(requisicao.categoriaId(), acumulado, this::buscarCategoria);
    }

    /** RN34: redução exige uma categoria do próprio usuário. */
    private Categoria buscarCategoria(Integer categoriaId) {
        if (categoriaId == null) {
            throw new DadosInvalidosException(MSG_REDUCAO_SEM_CATEGORIA);
        }
        return categorias.findByIdAndUsuarioId(categoriaId, usuarioLogado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(CategoriaService.MSG_NAO_ENCONTRADA));
    }

    private Meta buscarEntidade(Integer id) {
        return metas.findByIdAndUsuarioId(id, usuarioLogado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADA));
    }

    private YearMonth periodoDe(String mes) {
        return mes == null ? YearMonth.now(relogio) : YearMonth.parse(mes);
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Polimorfismo: cada filha monta a própria resposta; a de redução usa a fonte de gasto. */
    private MetaResponse paraResposta(Meta meta, YearMonth periodo) {
        return meta.paraResposta(periodo, this::gastoDaCategoria);
    }

    private BigDecimal gastoDaCategoria(Integer categoriaId, YearMonth periodo) {
        return lancamentos.somarPorCategoria(usuarioLogado.id(), TipoLancamento.DESPESA,
                        periodo.atDay(1), periodo.atEndOfMonth()).stream()
                .filter(gasto -> gasto.categoriaId().equals(categoriaId))
                .map(GastoPorCategoria::total)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
