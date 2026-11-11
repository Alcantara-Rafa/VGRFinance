package com.vgrfinance.service;

import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vgrfinance.dominio.MetodoOrcamentario;
import com.vgrfinance.dto.AjusteItemRequest;
import com.vgrfinance.dto.OrcamentoRequest;
import com.vgrfinance.dto.OrcamentoResponse;
import com.vgrfinance.exception.RecursoNaoEncontradoException;
import com.vgrfinance.exception.RegraDeNegocioException;
import com.vgrfinance.model.Categoria;
import com.vgrfinance.model.Orcamento;
import com.vgrfinance.model.TipoMetodo;
import com.vgrfinance.repository.CategoriaRepository;
import com.vgrfinance.repository.OrcamentoRepository;
import com.vgrfinance.seguranca.UsuarioLogado;

@Service
public class OrcamentoService {

    static final String MSG_NAO_ENCONTRADO = "Orçamento não encontrado.";
    static final String MSG_SEM_ORCAMENTO_NO_MES = "Não há orçamento para este mês.";
    static final String MSG_SEM_CATEGORIAS = "Cadastre ao menos uma categoria antes de criar o orçamento.";
    static final String MSG_CATEGORIA_ESCOLHIDA_INEXISTENTE = "Alguma das categorias escolhidas não existe.";
    static final String AVISO_SEM_ESSENCIAIS =
            "Nenhuma categoria essencial foi escolhida: a parte das essenciais ficou na reserva.";
    static final String AVISO_SEM_NAO_ESSENCIAIS =
            "Nenhuma categoria não essencial foi escolhida: essa parte ficou na reserva.";

    private final OrcamentoRepository orcamentos;
    private final CategoriaRepository categorias;
    private final UsuarioLogado usuarioLogado;
    private final MetodoOrcamentarioFactory fabrica;

    public OrcamentoService(OrcamentoRepository orcamentos, CategoriaRepository categorias,
                            UsuarioLogado usuarioLogado, MetodoOrcamentarioFactory fabrica) {
        this.orcamentos = orcamentos;
        this.categorias = categorias;
        this.usuarioLogado = usuarioLogado;
        this.fabrica = fabrica;
    }

    @Transactional
    public OrcamentoResponse criar(OrcamentoRequest requisicao) {
        YearMonth mes = YearMonth.parse(requisicao.mes());
        if (orcamentos.existsByUsuarioIdAndMesReferencia(usuarioLogado.id(), mes)) {
            throw new RegraDeNegocioException("Já existe um orçamento para " + mes + ". Edite ou exclua o existente.");
        }
        List<Categoria> escolhidas = categoriasDoPedido(requisicao);
        MetodoOrcamentario metodo = fabrica.criar(requisicao);

        Orcamento orcamento = new Orcamento(usuarioLogado.obter(), mes,
                requisicao.rendaBase().setScale(2, RoundingMode.HALF_EVEN));
        orcamento.gerar(metodo, escolhidas);          // polimorfismo: o orçamento não sabe qual regra é
        orcamentos.save(orcamento);
        return OrcamentoResponse.de(orcamento, avisosDe(requisicao.metodo(), escolhidas));
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResponse> listar() {
        return orcamentos.findByUsuarioIdOrderByMesReferenciaDesc(usuarioLogado.id()).stream()
                .map(o -> OrcamentoResponse.de(o, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoResponse buscarPorMes(String mes) {
        Orcamento orcamento = orcamentos.findByUsuarioIdAndMesReferencia(usuarioLogado.id(), YearMonth.parse(mes))
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_SEM_ORCAMENTO_NO_MES));
        return OrcamentoResponse.de(orcamento, List.of());
    }

    @Transactional
    public OrcamentoResponse ajustarItem(Integer orcamentoId, Integer categoriaId, AjusteItemRequest requisicao) {
        Orcamento orcamento = buscarEntidade(orcamentoId);
        orcamento.ajustarPrevisto(categoriaId, requisicao.valorPrevisto().setScale(2, RoundingMode.HALF_EVEN));
        return OrcamentoResponse.de(orcamento, List.of());
    }

    @Transactional
    public void excluir(Integer id) {
        orcamentos.delete(buscarEntidade(id));
    }

    private Orcamento buscarEntidade(Integer id) {
        return orcamentos.findByIdAndUsuarioId(id, usuarioLogado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADO));
    }

    private List<Categoria> categoriasDoPedido(OrcamentoRequest requisicao) {
        List<Categoria> todas = categorias.findByUsuarioIdOrderByNomeAsc(usuarioLogado.id());
        if (requisicao.categoriaIds() == null || requisicao.categoriaIds().isEmpty()) {
            if (todas.isEmpty()) {
                throw new RegraDeNegocioException(MSG_SEM_CATEGORIAS);
            }
            return todas;
        }
        Set<Integer> pedidas = new HashSet<>(requisicao.categoriaIds());
        List<Categoria> escolhidas = todas.stream().filter(c -> pedidas.contains(c.getId())).toList();
        if (escolhidas.size() != pedidas.size()) {
            throw new RecursoNaoEncontradoException(MSG_CATEGORIA_ESCOLHIDA_INEXISTENTE);
        }
        return escolhidas;
    }

    /** RN28: se um grupo não tem categorias, a parte dele fica na reserva e o usuário é avisado. */
    private static List<String> avisosDe(TipoMetodo metodo, List<Categoria> escolhidas) {
        List<String> avisos = new ArrayList<>();
        if (metodo == TipoMetodo.PROPORCIONAL) {
            if (escolhidas.stream().noneMatch(Categoria::isEssencial)) {
                avisos.add(AVISO_SEM_ESSENCIAIS);
            }
            if (escolhidas.stream().allMatch(Categoria::isEssencial)) {
                avisos.add(AVISO_SEM_NAO_ESSENCIAIS);
            }
        }
        return avisos;
    }
}
