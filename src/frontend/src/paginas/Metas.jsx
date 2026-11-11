import { useEffect, useState } from 'react';
import { api } from '../api.js';
import BarraPrevistoRealizado from '../componentes/BarraPrevistoRealizado.jsx';
import CampoMoeda from '../componentes/CampoMoeda.jsx';
import Mensagem from '../componentes/Mensagem.jsx';
import { mesPorExtenso, moeda, paraDecimal, paraDigitacao, percentual } from '../formato.js';

const TIPOS = [
  { id: 'ACUMULACAO', rotulo: 'Juntar dinheiro' },
  { id: 'REDUCAO_GASTO', rotulo: 'Gastar menos' },
];

// Cor nunca é o único sinal: cada situação tem ícone e rótulo.
const SITUACOES = {
  EM_ANDAMENTO: { icone: '●', rotulo: 'Em andamento', classe: 'neutro' },
  ATINGIDA: { icone: '✓', rotulo: 'Atingida', classe: 'positivo' },
  DENTRO_DO_TETO: { icone: '●', rotulo: 'Dentro do teto', classe: 'positivo' },
  TETO_ULTRAPASSADO: { icone: '◆', rotulo: 'Passou do teto', classe: 'negativo' },
};

const vazio = (tipo = 'ACUMULACAO') => ({ tipo, descricao: '', valorAlvo: '', categoriaId: '', valorAcumulado: '' });

function paraRequisicao(form) {
  const acumulacao = form.tipo === 'ACUMULACAO';
  return {
    tipo: form.tipo,
    descricao: form.descricao,
    valorAlvo: paraDecimal(form.valorAlvo),
    categoriaId: acumulacao ? null : Number(form.categoriaId),
    valorAcumulado: acumulacao && form.valorAcumulado ? paraDecimal(form.valorAcumulado) : null,
  };
}

export default function Metas({ aoIrPara }) {
  const [metas, setMetas] = useState(null);
  const [categorias, setCategorias] = useState([]);
  const [form, setForm] = useState(vazio);
  const [editando, setEditando] = useState(null);
  const [erro, setErro] = useState(null);

  const carregar = () => api.get('/metas').then(setMetas).catch((e) => setErro(e.message));

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
        await api.put(`/metas/${editando}`, paraRequisicao(form));
      } else {
        await api.post('/metas', paraRequisicao(form));
      }
      cancelar();
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  function editar(meta) {
    setEditando(meta.id);
    setForm({
      tipo: meta.tipo,
      descricao: meta.descricao,
      valorAlvo: paraDigitacao(meta.valorAlvo),
      categoriaId: meta.categoriaId ?? '',
      valorAcumulado: meta.valorAcumulado === null ? '' : paraDigitacao(meta.valorAcumulado),
    });
  }

  function cancelar() {
    setEditando(null);
    setForm(vazio(form.tipo));
  }

  async function excluir(meta) {
    setErro(null);
    try {
      await api.excluir(`/metas/${meta.id}`);
      await carregar();
    } catch (e) {
      setErro(e.message);
    }
  }

  const acumulacao = form.tipo === 'ACUMULACAO';

  return (
    <>
      <form className="cartao" onSubmit={salvar}>
        <div className="cabecalho-cartao">
          <h2>{editando ? 'Alterar meta' : 'Nova meta'}</h2>
          <div className="alternador" role="radiogroup" aria-label="Tipo da meta">
            {TIPOS.map((t) => (
              <label key={t.id} className={form.tipo === t.id ? 'ativo' : ''}>
                <input type="radio" name="tipo-meta" value={t.id} checked={form.tipo === t.id}
                       disabled={Boolean(editando)} onChange={() => setForm(vazio(t.id))} />
                {t.rotulo}
              </label>
            ))}
          </div>
        </div>
        <p className="secundario">
          {acumulacao
            ? 'Junte um valor para um objetivo, como uma reserva de emergência. Você registra o que guardar ao longo dos meses.'
            : 'Defina um teto mensal para uma categoria. O progresso compara o que já foi gasto no mês com esse teto.'}
        </p>

        <div className="grade-campos">
          <div>
            <label htmlFor="descricao-meta">Descrição</label>
            <input id="descricao-meta" required maxLength={255} value={form.descricao}
                   placeholder={acumulacao ? 'Ex.: reserva de emergência' : 'Ex.: delivery até R$ 250'}
                   onChange={(e) => mudar('descricao')(e.target.value)} />
          </div>
          <CampoMoeda id="valor-alvo" rotulo={acumulacao ? 'Valor a juntar' : 'Teto mensal'}
                      valor={form.valorAlvo} aoMudar={mudar('valorAlvo')} />
          {acumulacao ? (
            <CampoMoeda id="valor-acumulado" rotulo="Já guardado (opcional)" obrigatorio={false}
                        valor={form.valorAcumulado} aoMudar={mudar('valorAcumulado')} />
          ) : (
            <div>
              <label htmlFor="categoria-meta">Categoria</label>
              <select id="categoria-meta" required value={form.categoriaId}
                      onChange={(e) => mudar('categoriaId')(e.target.value)}>
                <option value="">Escolha…</option>
                {categorias.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
              </select>
            </div>
          )}
        </div>

        <div className="acoes-form">
          {editando && <button type="button" onClick={cancelar}>Cancelar</button>}
          <button className="primario">{editando ? 'Salvar alterações' : 'Criar meta'}</button>
        </div>
        <Mensagem erro={erro} />
      </form>

      {metas !== null && metas.length === 0 && (
        <div className="cartao vazio">
          <p>Você ainda não tem metas. Comece por uma reserva de emergência ou por um teto para a categoria que mais pesa.</p>
          <button onClick={() => aoIrPara('diagnostico')}>Ver o diagnóstico do mês</button>
        </div>
      )}

      {metas !== null && metas.length > 0 && (
        <section className="metas">
          {metas.map((meta) => (
            <CartaoMeta key={meta.id} meta={meta} aoMudar={carregar} aoEditar={editar} aoExcluir={excluir} />
          ))}
        </section>
      )}
    </>
  );
}

function CartaoMeta({ meta, aoMudar, aoEditar, aoExcluir }) {
  const [aporte, setAporte] = useState('');
  const [erro, setErro] = useState(null);
  const situacao = SITUACOES[meta.situacao];
  const acumulacao = meta.tipo === 'ACUMULACAO';

  async function guardar(evento) {
    evento.preventDefault();
    setErro(null);
    try {
      await api.post(`/metas/${meta.id}/aportes`, { valor: paraDecimal(aporte) });
      setAporte('');
      await aoMudar();
    } catch (e) {
      setErro(e.message);
    }
  }

  // Na acumulação, passar do alvo é bom: a barra para em 100% e não mostra excedente.
  const progressoDaBarra = acumulacao ? String(Math.min(Number(meta.progresso), 1)) : meta.progresso;
  const situacaoDaBarra = meta.situacao === 'TETO_ULTRAPASSADO' ? 'ESTOURADO' : 'NO_PLANO';

  return (
    <article className="cartao meta">
      <div className="acompanhamento-topo">
        <h3 className="principal">{meta.descricao}</h3>
        <span className={`selo-situacao ${situacao.classe}`}>
          <span aria-hidden="true">{situacao.icone}</span> {situacao.rotulo}
        </span>
      </div>
      <p className="secundario">
        {acumulacao
          ? `${moeda(meta.valorAcumulado)} de ${moeda(meta.valorAlvo)}`
          : `${meta.categoriaNome}: ${moeda(meta.gastoDoMes)} de ${moeda(meta.valorAlvo)} em ${mesPorExtenso(meta.mes)}`}
      </p>
      <BarraPrevistoRealizado percentual={progressoDaBarra} situacao={situacaoDaBarra} />
      <p className="meta-resumo">
        <span className="num">{percentual(meta.progresso)}</span>
        <span className="secundario">
          {acumulacao && (meta.situacao === 'ATINGIDA' ? 'Meta atingida.' : `Faltam ${moeda(meta.faltante)}.`)}
          {!acumulacao && (meta.situacao === 'TETO_ULTRAPASSADO'
            ? `Passou ${moeda(meta.excedente)} do teto.`
            : `Ainda cabem ${moeda(meta.economia)}.`)}
        </span>
      </p>

      {acumulacao && (
        <form className="linha-form" onSubmit={guardar}>
          <CampoMoeda id={`aporte-${meta.id}`} rotulo="Guardar mais" valor={aporte} aoMudar={setAporte} />
          <button>Guardar</button>
        </form>
      )}
      <Mensagem erro={erro} />
      <div className="acoes-form">
        <button className="discreto" onClick={() => aoEditar(meta)}>Alterar</button>
        <button className="discreto" onClick={() => aoExcluir(meta)}>Excluir</button>
      </div>
    </article>
  );
}
