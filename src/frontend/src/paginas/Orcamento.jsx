import { useEffect, useState } from 'react';
import { api } from '../api.js';
import CampoMoeda from '../componentes/CampoMoeda.jsx';
import Mensagem from '../componentes/Mensagem.jsx';
import { mesAtual, mesPorExtenso, moeda, paraDecimal, paraDigitacao } from '../formato.js';

const METODOS = [
  {
    id: 'PROPORCIONAL',
    titulo: 'Regra proporcional',
    texto: 'Divide sua renda em partes fixas. Por padrão, 50% vai para o que é essencial, 30% para o que '
      + 'não é e 20% fica reservado para metas e poupança.',
    dica: 'É um ponto de partida: você pode mudar qualquer valor.',
  },
  {
    id: 'BASE_ZERO',
    titulo: 'Base zero',
    texto: 'Cada real da renda ganha um destino. A sugestão divide a renda em partes iguais e você '
      + 'ajusta até a reserva chegar a zero.',
  },
];

const PERCENTUAIS_PADRAO = { pctEssenciais: '50', pctNaoEssenciais: '30', pctMetas: '20' };

export default function Orcamento({ aoIrPara }) {
  const [mes, setMes] = useState(mesAtual());
  const [orcamento, setOrcamento] = useState(undefined);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    setErro(null);
    setOrcamento(undefined);
    api.get(`/orcamentos?mes=${mes}`)
      .then(setOrcamento)
      .catch((e) => (e.status === 404 ? setOrcamento(null) : setErro(e.message)));
  }, [mes]);

  async function excluir() {
    setErro(null);
    try {
      await api.excluir(`/orcamentos/${orcamento.id}`);
      setOrcamento(null);
    } catch (e) {
      setErro(e.message);
    }
  }

  return (
    <>
      <h1>Orçamento de {mesPorExtenso(mes)}</h1>
      <div className="cartao linha-form">
        <div>
          <label htmlFor="mes">Mês</label>
          <input id="mes" type="month" required value={mes} onChange={(e) => e.target.value && setMes(e.target.value)} />
        </div>
      </div>
      <Mensagem erro={erro} />
      {orcamento === null && <NovoOrcamento mes={mes} aoCriar={setOrcamento} aoIrPara={aoIrPara} />}
      {orcamento && <AjusteDoOrcamento orcamento={orcamento} aoMudar={setOrcamento} aoExcluir={excluir} />}
    </>
  );
}

function NovoOrcamento({ mes, aoCriar, aoIrPara }) {
  const [renda, setRenda] = useState('');
  const [metodo, setMetodo] = useState('PROPORCIONAL');
  const [personalizar, setPersonalizar] = useState(false);
  const [percentuais, setPercentuais] = useState(PERCENTUAIS_PADRAO);
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);

  async function criar(evento) {
    evento.preventDefault();
    setErro(null);
    setEnviando(true);
    const usarPercentuais = metodo === 'PROPORCIONAL' && personalizar;
    try {
      aoCriar(await api.post('/orcamentos', {
        mes,
        rendaBase: paraDecimal(renda),
        metodo,
        ...(usarPercentuais && {
          pctEssenciais: paraDecimal(percentuais.pctEssenciais),
          pctNaoEssenciais: paraDecimal(percentuais.pctNaoEssenciais),
          pctMetas: paraDecimal(percentuais.pctMetas),
        }),
      }));
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <form className="cartao" onSubmit={criar}>
      <h2>Crie o orçamento do mês</h2>
      <p className="secundario">Informe quanto entra no mês e escolha como dividir. Você ajusta tudo depois.</p>
      <div className="linha-form">
        <CampoMoeda id="renda" rotulo="Renda do mês" valor={renda} aoMudar={setRenda} />
      </div>

      <fieldset className="metodos">
        <legend className="secundario">Método</legend>
        {METODOS.map((m) => (
          <label key={m.id} className={`metodo ${metodo === m.id ? 'escolhido' : ''}`}>
            <input type="radio" name="metodo" value={m.id} checked={metodo === m.id}
                   onChange={() => setMetodo(m.id)} />
            <strong>{m.titulo}</strong>
            <span>{m.texto}</span>
            {m.dica && <span className="mensagem dica">{m.dica}</span>}
          </label>
        ))}
      </fieldset>

      {metodo === 'PROPORCIONAL' && (
        <>
          <label className="check">
            <input type="checkbox" checked={personalizar} onChange={(e) => setPersonalizar(e.target.checked)} />
            Usar outros percentuais (precisam somar 100)
          </label>
          {personalizar && (
            <div className="linha-form">
              {[['pctEssenciais', 'Essenciais (%)'], ['pctNaoEssenciais', 'Não essenciais (%)'],
                ['pctMetas', 'Metas e poupança (%)']].map(([campo, rotulo]) => (
                <div key={campo}>
                  <label htmlFor={campo}>{rotulo}</label>
                  <input id={campo} className="num" inputMode="decimal" required value={percentuais[campo]}
                         onChange={(e) => setPercentuais({ ...percentuais, [campo]: e.target.value })} />
                </div>
              ))}
            </div>
          )}
        </>
      )}

      <div className="linha-form">
        <button className="primario" disabled={enviando}>{enviando ? 'Calculando…' : 'Criar orçamento'}</button>
        {erro?.includes('categoria') && (
          <button type="button" onClick={() => aoIrPara('categorias')}>Ir para categorias</button>
        )}
      </div>
      <Mensagem erro={erro} />
    </form>
  );
}

function AjusteDoOrcamento({ orcamento, aoMudar, aoExcluir }) {
  const [digitados, setDigitados] = useState({});
  const [calculando, setCalculando] = useState(null);
  const [erro, setErro] = useState(null);

  // Sempre que a API devolve o orçamento, os campos mostram os valores dela.
  useEffect(() => {
    setDigitados(Object.fromEntries(orcamento.itens.map((i) => [i.categoriaId, paraDigitacao(i.valorPrevisto)])));
  }, [orcamento]);

  async function ajustar(item) {
    const digitado = digitados[item.categoriaId] ?? '';
    setErro(null);
    if (digitado === paraDigitacao(item.valorPrevisto)) {
      return;
    }
    setCalculando(item.categoriaId);
    try {
      const valorPrevisto = paraDecimal(digitado);
      aoMudar(await api.put(`/orcamentos/${orcamento.id}/itens/${item.categoriaId}`, { valorPrevisto }));
    } catch (e) {
      setErro(e.message);
      setDigitados({ ...digitados, [item.categoriaId]: paraDigitacao(item.valorPrevisto) });
    } finally {
      setCalculando(null);
    }
  }

  const reservaPositiva = Number(orcamento.reserva) > 0;

  return (
    <section className="cartao">
      <h2>{orcamento.metodoNome} · renda de <span className="num">{moeda(orcamento.rendaBase)}</span></h2>
      {orcamento.avisos.map((aviso) => <p key={aviso} className="mensagem dica">{aviso}</p>)}

      <ul className="lista itens">
        {orcamento.itens.map((item) => (
          <li key={item.categoriaId}>
            <label className="principal" htmlFor={`item-${item.categoriaId}`}>
              {item.categoriaNome}
              {item.essencial && <span className="selo">essencial</span>}
            </label>
            {calculando === item.categoriaId && <span className="secundario">calculando…</span>}
            <div className="moeda">
              <span aria-hidden="true">R$</span>
              <input
                id={`item-${item.categoriaId}`}
                inputMode="decimal"
                value={digitados[item.categoriaId] ?? ''}
                onChange={(e) => setDigitados({ ...digitados, [item.categoriaId]: e.target.value })}
                onBlur={() => ajustar(item)}
                onKeyDown={(e) => e.key === 'Enter' && e.currentTarget.blur()}
              />
            </div>
          </li>
        ))}
      </ul>
      <Mensagem erro={erro} />

      <div className="totais">
        <span>Total previsto <strong className="num">{moeda(orcamento.totalPrevisto)}</strong></span>
        <span>
          Reserva <strong className={`num ${reservaPositiva ? 'positivo' : ''}`}>{moeda(orcamento.reserva)}</strong>
          <span className="secundario">
            {orcamento.zerado ? ' ● cada real tem destino' : ' ● sobra sem destino, para metas e poupança'}
          </span>
        </span>
        <button className="discreto" onClick={aoExcluir}>Excluir orçamento</button>
      </div>
    </section>
  );
}
