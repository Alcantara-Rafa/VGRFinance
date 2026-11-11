import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Mensagem from '../componentes/Mensagem.jsx';
import { mesAtual, mesPorExtenso, moeda, percentual } from '../formato.js';

// Cor nunca é o único sinal: cada faixa tem ícone e rótulo (o rótulo vem da API).
const FAIXAS = {
  SAUDAVEL: { icone: '●', classe: 'positivo' },
  ATENCAO: { icone: '▲', classe: 'atencao' },
  CRITICA: { icone: '◆', classe: 'negativo' },
};

export default function Diagnostico({ aoIrPara }) {
  const [mes, setMes] = useState(mesAtual());
  const [diagnostico, setDiagnostico] = useState(undefined);
  // O 409 não é erro: é um convite para o passo que falta (orçamento ou gasto).
  const [convite, setConvite] = useState(null);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    setErro(null);
    setConvite(null);
    setDiagnostico(undefined);
    api.get(`/diagnosticos?mes=${mes}`)
      .then(setDiagnostico)
      .catch((e) => (e.status === 409 ? setConvite(e.message) : setErro(e.message)));
  }, [mes]);

  const faltaOrcamento = convite?.includes('orçamento');

  return (
    <>
      <div className="cartao cabecalho-cartao">
        <h2>Diagnóstico de {mesPorExtenso(mes)}</h2>
        <div className="campo-mes">
          <label htmlFor="mes-diagnostico">Mês</label>
          <input id="mes-diagnostico" type="month" required value={mes}
                 onChange={(e) => e.target.value && setMes(e.target.value)} />
        </div>
      </div>
      <Mensagem erro={erro} />

      {convite && (
        <div className="cartao vazio">
          <p>{convite}</p>
          <button className="primario" onClick={() => aoIrPara(faltaOrcamento ? 'orcamento' : 'lancamentos')}>
            {faltaOrcamento ? 'Criar orçamento' : 'Registrar um gasto'}
          </button>
        </div>
      )}

      {diagnostico && (
        <>
          <section className="resumo">
            <div className="cartao resumo-item">
              <span className="secundario">Renda do mês</span>
              <strong className="num">{moeda(diagnostico.rendaLiquida)}</strong>
              <span className="secundario">a renda informada no orçamento</span>
            </div>
            <div className="cartao resumo-item">
              <span className="secundario">Despesas do mês</span>
              <strong className="num">{moeda(diagnostico.despesaTotal)}</strong>
              <span className="secundario">tudo o que saiu como despesa</span>
            </div>
            <div className="cartao resumo-item">
              <span className="secundario">Sobra</span>
              <strong className={`num ${Number(diagnostico.sobra) < 0 ? 'negativo' : 'positivo'}`}>
                {moeda(diagnostico.sobra)}
              </strong>
              <span className="secundario">renda menos despesas</span>
            </div>
          </section>

          <section className="indicadores">
            {diagnostico.indicadores.map((indicador) => {
              const faixa = FAIXAS[indicador.faixa];
              return (
                <article key={indicador.codigo} className="cartao indicador">
                  <div className="acompanhamento-topo">
                    <h3 className="principal">{indicador.titulo}</h3>
                    <span className={`selo-situacao ${faixa.classe}`}>
                      <span aria-hidden="true">{faixa.icone}</span> {indicador.rotuloFaixa}
                    </span>
                  </div>
                  <strong className="indicador-valor num">{percentual(indicador.valor)}</strong>
                  <div className="significado">
                    <span className="significado-titulo">O que isso significa?</span>
                    <p>{indicador.significado}</p>
                  </div>
                  <p><strong>Próximo passo:</strong> {indicador.proximoPasso}</p>
                </article>
              );
            })}
          </section>

          <p className="secundario aviso-educativo">
            O diagnóstico é educativo e não é consultoria financeira. As faixas são referências do VGRFinance.
          </p>
        </>
      )}
    </>
  );
}
