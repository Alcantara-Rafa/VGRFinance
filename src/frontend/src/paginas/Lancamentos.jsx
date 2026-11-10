import { useEffect, useRef, useState } from 'react';
import { api } from '../api.js';
import CampoMoeda from '../componentes/CampoMoeda.jsx';
import Mensagem from '../componentes/Mensagem.jsx';
import { data, moedaComSinal, paraDecimal, paraDigitacao } from '../formato.js';

const VAZIO = { valor: '', categoriaId: '', tipo: 'DESPESA', data: '', descricao: '' };

/** Monta o corpo da API: campos vazios não vão, e a API completa o resto (lançamento rápido). */
function paraRequisicao(form) {
  return {
    valor: paraDecimal(form.valor),
    categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
    tipo: form.tipo,
    data: form.data || null,
    descricao: form.descricao || null,
  };
}

export default function Lancamentos({ aoIrPara }) {
  const [lancamentos, setLancamentos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [form, setForm] = useState(VAZIO);
  const [editando, setEditando] = useState(null);
  const [erro, setErro] = useState(null);
  const campoValor = useRef(null);

  const carregar = () => api.get('/lancamentos').then(setLancamentos).catch((e) => setErro(e.message));

  useEffect(() => {
    carregar();
    api.get('/categorias').then(setCategorias).catch((e) => setErro(e.message));
  }, []);

  const mudar = (campo) => (valor) => setForm({ ...form, [campo]: valor });

  async function salvar(evento) {
    evento.preventDefault();
    setErro(null);
    try {
      if (editando) {
        await api.put(`/lancamentos/${editando}`, paraRequisicao(form));
      } else {
        await api.post('/lancamentos', paraRequisicao(form));
      }
      // Mantém a categoria para o próximo gasto e devolve o foco ao valor.
      setForm({ ...VAZIO, categoriaId: editando ? '' : form.categoriaId });
      setEditando(null);
      await carregar();
      campoValor.current?.focus();
    } catch (e) {
      setErro(e.message);
    }
  }

  function editar(l) {
    setEditando(l.id);
    setForm({
      valor: paraDigitacao(l.valor),
      categoriaId: l.categoriaId ?? '',
      tipo: l.tipo,
      data: l.data,
      descricao: l.descricao ?? '',
    });
    campoValor.current?.focus();
  }

  function cancelar() {
    setEditando(null);
    setForm(VAZIO);
  }

  async function excluir(l) {
    setErro(null);
    try {
      await api.excluir(`/lancamentos/${l.id}`);
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  if (categorias.length === 0 && lancamentos.length === 0) {
    return (
      <>
        <h1>Lançamentos</h1>
        <div className="cartao vazio">
          <p>Para registrar um gasto, primeiro crie suas categorias.</p>
          <button className="primario" onClick={() => aoIrPara('categorias')}>Criar categorias</button>
        </div>
      </>
    );
  }

  return (
    <>
      <h1>Lançamentos</h1>
      <form className="cartao" onSubmit={salvar}>
        <h2>{editando ? 'Alterar lançamento' : 'Registrar gasto'}</h2>
        <div className="linha-form">
          <CampoMoeda id="valor" rotulo="Valor" ref={campoValor} valor={form.valor} aoMudar={mudar('valor')} />
          <div>
            <label htmlFor="categoria">Categoria</label>
            <select id="categoria" value={form.categoriaId} onChange={(e) => mudar('categoriaId')(e.target.value)}>
              <option value="">{form.tipo === 'RECEITA' ? 'Sem categoria' : 'Escolha…'}</option>
              {categorias.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </div>
          <button className="primario">{editando ? 'Salvar alterações' : 'Salvar'}</button>
          {editando && <button type="button" onClick={cancelar}>Cancelar</button>}
        </div>
        <details open={Boolean(editando)}>
          <summary>Mais detalhes (tipo, data, descrição)</summary>
          <div className="linha-form">
            <div>
              <label htmlFor="tipo">Tipo</label>
              <select id="tipo" value={form.tipo} onChange={(e) => mudar('tipo')(e.target.value)}>
                <option value="DESPESA">Despesa</option>
                <option value="RECEITA">Receita</option>
              </select>
            </div>
            <div>
              <label htmlFor="data">Data (vazio = hoje)</label>
              <input id="data" type="date" value={form.data} onChange={(e) => mudar('data')(e.target.value)} />
            </div>
            <div>
              <label htmlFor="descricao">Descrição</label>
              <input id="descricao" maxLength={120} value={form.descricao}
                     onChange={(e) => mudar('descricao')(e.target.value)} />
            </div>
          </div>
        </details>
        <Mensagem erro={erro} />
      </form>

      <section className="cartao">
        {lancamentos.length === 0 ? (
          <p className="vazio">Nenhum lançamento ainda. Registre o primeiro gasto acima: valor, categoria e Enter.</p>
        ) : (
          <ul className="lista">
            {lancamentos.map((l) => (
              <li key={l.id}>
                <span className="principal">
                  {l.categoriaNome ?? 'Receita'}
                  <span className="secundario"> · {data(l.data)}{l.descricao && ` · ${l.descricao}`}</span>
                </span>
                <span className={`num ${l.tipo === 'RECEITA' ? 'positivo' : ''}`}>
                  {moedaComSinal(l.valor, l.tipo === 'RECEITA')}
                </span>
                <span className="acoes">
                  <button className="discreto" onClick={() => editar(l)}>Alterar</button>
                  <button className="discreto" onClick={() => excluir(l)}>Excluir</button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
