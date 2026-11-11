import { useEffect, useRef, useState } from 'react';
import { api } from '../api.js';
import CampoMoeda from '../componentes/CampoMoeda.jsx';
import Mensagem from '../componentes/Mensagem.jsx';
import { data, hoje, mesAtual, mesPorExtenso, moeda, moedaComSinal, paraDecimal, paraDigitacao, percentual }
  from '../formato.js';

const TIPOS = [
  { id: 'DESPESA', rotulo: 'Despesa' },
  { id: 'RECEITA', rotulo: 'Receita' },
];

const vazio = () => ({ valor: '', categoriaId: '', tipo: 'DESPESA', data: hoje(), descricao: '' });

/** Aviso depois de uma despesa: só quando a categoria chegou a 80% do previsto ou passou dele. */
async function avisoDaCategoria(mes, categoriaId) {
  try {
    const acompanhamento = await api.get(`/acompanhamento?mes=${mes}`);
    const item = acompanhamento.itens.find((i) => i.categoriaId === categoriaId);
    if (item?.situacao === 'ATENCAO') {
      return { tipo: 'atencao', texto: `${item.categoriaNome} chegou a ${percentual(item.percentualUtilizado)} do previsto: `
        + `${moeda(item.realizado)} de ${moeda(item.previsto)}.` };
    }
    if (item?.situacao === 'ESTOURADO') {
      return { tipo: 'erro', texto: `${item.categoriaNome} passou ${moeda(item.excedente)} do previsto. `
        + 'Se quiser, aumente o previsto ou reduza em outra categoria.' };
    }
  } catch {
    // Mês sem orçamento: não há o que comparar, então não há aviso.
  }
  return null;
}

/** Corpo da API: o que vier vazio a API completa (lançamento rápido). */
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
  // null = ainda carregando as categorias.
  const [categorias, setCategorias] = useState(null);
  const [mesHistorico, setMesHistorico] = useState(mesAtual());
  const [aviso, setAviso] = useState(null);
  const [form, setForm] = useState(vazio);
  const [editando, setEditando] = useState(null);
  const [erro, setErro] = useState(null);
  const campoValor = useRef(null);

  const carregar = (mes) => api.get(`/lancamentos?mes=${mes}`).then(setLancamentos).catch((e) => setErro(e.message));

  useEffect(() => {
    api.get('/categorias').then(setCategorias).catch((e) => setErro(e.message));
  }, []);

  useEffect(() => {
    carregar(mesHistorico);
  }, [mesHistorico]);

  const mudar = (campo) => (valor) => setForm({ ...form, [campo]: valor });

  async function salvar(evento) {
    evento.preventDefault();
    setErro(null);
    setAviso(null);
    try {
      if (editando) {
        await api.put(`/lancamentos/${editando}`, paraRequisicao(form));
      } else {
        await api.post('/lancamentos', paraRequisicao(form));
      }
      // Mantém tipo e categoria para o próximo lançamento e devolve o foco ao valor.
      setForm({ ...vazio(), tipo: form.tipo, categoriaId: editando ? '' : form.categoriaId });
      setEditando(null);
      // O histórico vai para o mês do lançamento salvo, para ele aparecer na lista.
      const mesSalvo = form.data.slice(0, 7);
      setMesHistorico(mesSalvo);
      await carregar(mesSalvo);
      if (form.tipo === 'DESPESA' && form.categoriaId) {
        setAviso(await avisoDaCategoria(mesSalvo, Number(form.categoriaId)));
      }
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
    setForm(vazio());
  }

  async function excluir(l) {
    setErro(null);
    try {
      await api.excluir(`/lancamentos/${l.id}`);
      await carregar(mesHistorico);
    } catch (e) {
      setErro(e.message);
    }
  }

  if (categorias === null) {
    return null;
  }
  if (categorias.length === 0) {
    return (
      <div className="cartao vazio">
        <p>Para começar, crie suas categorias. Elas organizam os lançamentos e o orçamento.</p>
        <button className="primario" onClick={() => aoIrPara('categorias')}>Criar categorias</button>
      </div>
    );
  }

  const receita = form.tipo === 'RECEITA';

  return (
    <>
      <form className="cartao" onSubmit={salvar}>
        <div className="cabecalho-cartao">
          <h2>{editando ? 'Alterar lançamento' : 'Novo lançamento'}</h2>
          <div className="alternador" role="radiogroup" aria-label="Tipo do lançamento">
            {TIPOS.map((t) => (
              <label key={t.id} className={form.tipo === t.id ? 'ativo' : ''}>
                <input type="radio" name="tipo" value={t.id} checked={form.tipo === t.id}
                       onChange={() => mudar('tipo')(t.id)} />
                {t.rotulo}
              </label>
            ))}
          </div>
        </div>

        <div className="grade-campos">
          <CampoMoeda id="valor" rotulo="Valor" ref={campoValor} valor={form.valor} aoMudar={mudar('valor')} />
          <div>
            <label htmlFor="categoria">Categoria{receita && ' (opcional)'}</label>
            <select id="categoria" value={form.categoriaId} onChange={(e) => mudar('categoriaId')(e.target.value)}>
              <option value="">{receita ? 'Sem categoria' : 'Escolha…'}</option>
              {categorias.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="data">Data</label>
            <input id="data" type="date" required value={form.data} onChange={(e) => mudar('data')(e.target.value)} />
          </div>
          <div className="campo-largo">
            <label htmlFor="descricao">Descrição (opcional)</label>
            <input id="descricao" maxLength={120} placeholder={receita ? 'Ex.: salário de novembro' : 'Ex.: mercado da semana'}
                   value={form.descricao} onChange={(e) => mudar('descricao')(e.target.value)} />
          </div>
        </div>

        <div className="acoes-form">
          {editando && <button type="button" onClick={cancelar}>Cancelar</button>}
          <button className="primario">{editando ? 'Salvar alterações' : `Registrar ${receita ? 'receita' : 'despesa'}`}</button>
        </div>
        <Mensagem erro={erro} />
        {aviso && <p className={`mensagem ${aviso.tipo}`} role="status">{aviso.texto}</p>}
      </form>

      <section className="cartao">
        <div className="cabecalho-cartao">
          <h2>Histórico de {mesPorExtenso(mesHistorico)}</h2>
          <div className="campo-mes">
            <label htmlFor="mes-historico">Mês</label>
            <input id="mes-historico" type="month" required value={mesHistorico}
                   onChange={(e) => e.target.value && setMesHistorico(e.target.value)} />
          </div>
        </div>
        {lancamentos.length === 0 ? (
          <p className="vazio">Nenhum lançamento neste mês. Escolha o tipo, informe o valor e pressione Enter.</p>
        ) : (
          <ul className="lista">
            {lancamentos.map((l) => (
              <li key={l.id}>
                <span className={`marcador ${l.tipo === 'RECEITA' ? 'receita' : ''}`} aria-hidden="true" />
                <span className="principal">
                  {l.categoriaNome ?? 'Receita'}
                  <span className="secundario">{data(l.data)}{l.descricao && ` · ${l.descricao}`}</span>
                </span>
                <span className={`num valor ${l.tipo === 'RECEITA' ? 'positivo' : ''}`}>
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
