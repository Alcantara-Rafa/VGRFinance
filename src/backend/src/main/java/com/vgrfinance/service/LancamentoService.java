package com.vgrfinance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.dto.LancamentoRequest;
import com.vgrfinance.dto.LancamentoResponse;
import com.vgrfinance.exception.DadosInvalidosException;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Lancamento;
import com.vgrfinance.model.TipoLancamento;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@Service
public class LancamentoService {

    static final String MSG_NAO_ENCONTRADO = "Lançamento não encontrado.";
    static final String MSG_DESPESA_SEM_CATEGORIA = "Escolha a categoria da despesa.";

    private final LancamentoRepository lancamentos;
    private final CategoriaRepository categorias;
    private final UsuarioLogado usuarioLogado;
    private final Clock relogio;

    public LancamentoService(LancamentoRepository lancamentos, CategoriaRepository categorias,
                             UsuarioLogado usuarioLogado, Clock relogio) {
        this.lancamentos = lancamentos;
        this.categorias = categorias;
        this.usuarioLogado = usuarioLogado;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponse> listar() {
        return lancamentos.findByUsuarioIdOrderByDataDescIdDesc(usuarioLogado.id()).stream()
                .map(LancamentoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public LancamentoResponse buscar(Integer id) {
        return LancamentoResponse.de(buscarEntidade(id));
    }

    /** RN20, lançamento rápido: sem tipo vale DESPESA, sem data vale hoje, descrição vazia vira nula. */
    @Transactional
    public LancamentoResponse criar(LancamentoRequest requisicao) {
        TipoLancamento tipo = tipoDe(requisicao);
        Lancamento lancamento = new Lancamento(usuarioLogado.obter(), categoriaDe(requisicao, tipo), tipo,
                normalizar(requisicao.valor()), dataDe(requisicao), descricaoDe(requisicao));
        return LancamentoResponse.de(lancamentos.save(lancamento));
    }

    @Transactional
    public LancamentoResponse atualizar(Integer id, LancamentoRequest requisicao) {
        Lancamento lancamento = buscarEntidade(id);
        TipoLancamento tipo = tipoDe(requisicao);
        lancamento.atualizar(categoriaDe(requisicao, tipo), tipo,
                normalizar(requisicao.valor()), dataDe(requisicao), descricaoDe(requisicao));
        return LancamentoResponse.de(lancamento);
    }

    @Transactional
    public void excluir(Integer id) {
        lancamentos.delete(buscarEntidade(id));
    }

    private Lancamento buscarEntidade(Integer id) {
        return lancamentos.findByIdAndUsuarioId(id, usuarioLogado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADO));
    }

    private static TipoLancamento tipoDe(LancamentoRequest requisicao) {
        return requisicao.tipo() == null ? TipoLancamento.DESPESA : requisicao.tipo();
    }

    /** Só a despesa exige categoria. RN23: a categoria precisa existir e ser do usuário. */
    private Categoria categoriaDe(LancamentoRequest requisicao, TipoLancamento tipo) {
        if (requisicao.categoriaId() == null) {
            if (tipo == TipoLancamento.DESPESA) {
                throw new DadosInvalidosException(MSG_DESPESA_SEM_CATEGORIA);
            }
            return null;
        }
        return categorias.findByIdAndUsuarioId(requisicao.categoriaId(), usuarioLogado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(CategoriaService.MSG_NAO_ENCONTRADA));
    }

    /** RN21: "hoje" vem do relógio de Brasília, nunca de LocalDate.now() direto. */
    private LocalDate dataDe(LancamentoRequest requisicao) {
        return requisicao.data() == null ? LocalDate.now(relogio) : requisicao.data();
    }

    private static String descricaoDe(LancamentoRequest requisicao) {
        String descricao = requisicao.descricao();
        return descricao == null || descricao.isBlank() ? null : descricao.strip();
    }

    /** RN10: dinheiro com escala 2. */
    private static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
