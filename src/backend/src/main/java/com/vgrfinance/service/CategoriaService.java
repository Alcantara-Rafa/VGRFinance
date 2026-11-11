package com.vgrfinance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.dto.CategoriaRequest;
import com.vgrfinance.dto.CategoriaResponse;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.ItemOrcamentoRepository;
import com.vgrfinance.repository.LancamentoRepository;
import com.vgrfinance.repository.MetaRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@Service
public class CategoriaService {

    static final String MSG_NAO_ENCONTRADA = "Categoria não encontrada.";
    static final String MSG_NOME_REPETIDO = "Já existe uma categoria com este nome.";
    static final String MSG_EM_USO = "Não é possível excluir esta categoria porque ela está em lançamentos "
            + "em algum orçamento ou em alguma meta. Mova os lançamentos ou ajuste o orçamento e as metas antes.";
    static final String MSG_PADRAO_RENOMEAR = "O nome de uma categoria padrão não pode ser alterado.";
    static final String MSG_PADRAO_EXCLUIR = "Categorias padrão não podem ser excluídas.";

    private final CategoriaRepository categorias;
    private final LancamentoRepository lancamentos;
    private final ItemOrcamentoRepository itensOrcamento;
    private final MetaRepository metas;
    private final UsuarioLogado usuarioLogado;

    public CategoriaService(CategoriaRepository categorias, LancamentoRepository lancamentos,
                            ItemOrcamentoRepository itensOrcamento, MetaRepository metas,
                            UsuarioLogado usuarioLogado) {
        this.categorias = categorias;
        this.lancamentos = lancamentos;
        this.itensOrcamento = itensOrcamento;
        this.metas = metas;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categorias.findByUsuarioIdOrderByNomeAsc(usuarioLogado.id()).stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Integer id) {
        return CategoriaResponse.de(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest requisicao) {
        String nome = requisicao.nome().strip();
        if (categorias.existsByUsuarioIdAndNomeIgnoreCase(usuarioLogado.id(), nome)) {
            throw new RegraDeNegocioException(MSG_NOME_REPETIDO);
        }
        Categoria categoria = new Categoria(usuarioLogado.obter(), nome, requisicao.essencial());
        return CategoriaResponse.de(categorias.save(categoria));
    }

    /** Categoria padrão: só o "essencial" muda; o nome fica como o sistema criou. */
    @Transactional
    public CategoriaResponse atualizar(Integer id, CategoriaRequest requisicao) {
        Categoria categoria = buscarEntidade(id);
        String nome = requisicao.nome().strip();
        if (categoria.isPadrao() && !nome.equals(categoria.getNome())) {
            throw new RegraDeNegocioException(MSG_PADRAO_RENOMEAR);
        }
        if (categorias.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(usuarioLogado.id(), nome, id)) {
            throw new RegraDeNegocioException(MSG_NOME_REPETIDO);
        }
        categoria.atualizar(nome, requisicao.essencial());
        return CategoriaResponse.de(categoria);
    }

    /**
     * Categoria padrão nunca é excluída.
     * RN09 (ampliada): categoria usada em lançamento, orçamento ou meta não pode ser excluída.
     */
    @Transactional
    public void excluir(Integer id) {
        Categoria categoria = buscarEntidade(id);
        if (categoria.isPadrao()) {
            throw new RegraDeNegocioException(MSG_PADRAO_EXCLUIR);
        }
        if (lancamentos.existsByCategoriaId(id) || itensOrcamento.existsByCategoriaId(id)
                || metas.existeMetaDaCategoria(id)) {
            throw new RegraDeNegocioException(MSG_EM_USO);
        }
        categorias.delete(categoria);
    }

    private Categoria buscarEntidade(Integer id) {
        return categorias.findByIdAndUsuarioId(id, usuarioLogado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADA));
    }
}
