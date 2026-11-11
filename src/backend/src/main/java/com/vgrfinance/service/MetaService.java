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
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Meta;
import com.vgrfinance.model.MetaAcumulacao;
import com.vgrfinance.model.MetaReducaoGasto;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.MetaRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

/** RF06: metas de acumulação e de redução de gasto. */
@Service
public class MetaService {

    static final String MSG_NAO_ENCONTRADA = "Meta não encontrada.";
    static final String MSG_TIPO_IMUTAVEL = "O tipo de uma meta não pode ser alterado. Crie uma nova meta.";
    static final String MSG_APORTE_EM_REDUCAO = "Metas de redução de gasto não recebem aportes.";
    static final String MSG_ACUMULACAO_COM_CATEGORIA = "Meta de acumulação não tem categoria.";
    static final String MSG_REDUCAO_COM_ACUMULADO = "Meta de redução de gasto não tem valor guardado.";
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

    @Transactional
    public MetaResponse criar(MetaRequest requisicao) {
        Meta meta = switch (requisicao.tipo()) {
            case ACUMULACAO -> novaAcumulacao(requisicao);
            case REDUCAO_GASTO -> novaReducao(requisicao);
        };
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
        if (meta instanceof MetaAcumulacao acumulacao) {
            exigirSemCategoria(requisicao);
            if (requisicao.valorAcumulado() != null) {
                acumulacao.corrigirValorAcumulado(dinheiro(requisicao.valorAcumulado()));
            }
        } else if (meta instanceof MetaReducaoGasto reducao) {
            exigirSemValorAcumulado(requisicao);
            reducao.alterarCategoria(buscarCategoria(requisicao.categoriaId()));
        }
        return paraResposta(meta, periodoDe(null));
    }

    /** RN32: só meta de acumulação recebe aporte. */
    @Transactional
    public MetaResponse aportar(Integer id, AporteRequest requisicao) {
        if (!(buscarEntidade(id) instanceof MetaAcumulacao acumulacao)) {
            throw new RegraDeNegocioException(MSG_APORTE_EM_REDUCAO);
        }
        acumulacao.registrarAporte(dinheiro(requisicao.valor()));
        return paraResposta(acumulacao, periodoDe(null));
    }

    @Transactional
    public void excluir(Integer id) {
        metas.delete(buscarEntidade(id));
    }

    private Meta novaAcumulacao(MetaRequest requisicao) {
        exigirSemCategoria(requisicao);
        MetaAcumulacao meta = new MetaAcumulacao(usuarioLogado.obter(), requisicao.descricao().strip(),
                dinheiro(requisicao.valorAlvo()));
        if (requisicao.valorAcumulado() != null) {
            meta.corrigirValorAcumulado(dinheiro(requisicao.valorAcumulado()));
        }
        return meta;
    }

    private Meta novaReducao(MetaRequest requisicao) {
        exigirSemValorAcumulado(requisicao);
        return new MetaReducaoGasto(usuarioLogado.obter(), requisicao.descricao().strip(),
                dinheiro(requisicao.valorAlvo()), buscarCategoria(requisicao.categoriaId()));
    }

    /** RN34: acumulação não tem categoria. */
    private static void exigirSemCategoria(MetaRequest requisicao) {
        if (requisicao.categoriaId() != null) {
            throw new DadosInvalidosException(MSG_ACUMULACAO_COM_CATEGORIA);
        }
    }

    private static void exigirSemValorAcumulado(MetaRequest requisicao) {
        if (requisicao.valorAcumulado() != null) {
            throw new DadosInvalidosException(MSG_REDUCAO_COM_ACUMULADO);
        }
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

    private MetaResponse paraResposta(Meta meta, YearMonth periodo) {
        if (meta instanceof MetaReducaoGasto reducao) {
            reducao.apurar(gastoDaCategoria(reducao.getCategoria().getId(), periodo));
        }
        // Polimorfismo: cada filha calcula progresso e situação do seu jeito.
        BigDecimal progresso = meta.calcularProgresso();
        String situacao = meta.getSituacao();

        if (meta instanceof MetaAcumulacao a) {
            return new MetaResponse(a.getId(), a.getTipo(), a.getDescricao(), a.getValorAlvo(), progresso, situacao,
                    a.getValorAcumulado(), a.getFaltante(), null, null, null, null, null, null);
        }
        MetaReducaoGasto r = (MetaReducaoGasto) meta;
        return new MetaResponse(r.getId(), r.getTipo(), r.getDescricao(), r.getValorAlvo(), progresso, situacao,
                null, null, r.getCategoria().getId(), r.getCategoria().getNome(), periodo.toString(),
                r.getGastoDoMes(), r.getEconomia(), r.getExcedente());
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
