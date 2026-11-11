import { useEffect, useState } from 'react';
import { api } from '../api.js';
import BarraPrevistoRealizado from '../componentes/BarraPrevistoRealizado.jsx';
import Mensagem from '../componentes/Mensagem.jsx';
import SeloSituacao from '../componentes/SeloSituacao.jsx';
import { mesAtual, mesPorExtenso, moeda, moedaSemSinal, percentual } from '../formato.js';

export default function Acompanhamento({ aoIrPara }) {
  const [mes, setMes] = useState(mesAtual());
  // undefined = carregando; null = o mês não tem orçamento.
  const [dados, setDados] = useState(undefined);
  const [erro, setErro] = useState(null);

  useEffect(() => {
    setErro(null);
    setDados(undefined);
    api.get(`/acompanhamento?mes=${mes}`)
      .then(setDados)
      .catch((e) => (e.status === 404 ? setDados(null) : setErro(e.message)));
  }, [mes]);

  return (
    <>
      <div className="cartao cabecalho-cartao">
        <h2>Acompanhamento de {mesPorExtenso(mes)}</h2>
        <div className="campo-mes">
          <label htmlFor="mes-acompanhamento">Mês</label>
          <input id="mes-acompanhamento" type="month" required value={mes}
                 onChange={(e) => e.target.value && setMes(e.target.value)} />
        </div>
      </div>
      <Mensagem erro={erro} />

      {dados === null && (
        <div className="cartao vazio">
          <p>Este mês ainda não tem orçamento. Crie um para comparar o previsto com o que você gastou.</p>
          <button className="primario" onClick={() => aoIrPara('orcamento')}>Criar orçamento</button>
        </div>
      )}

      {dados && (
        <>
          <section className="resumo">
            <div className="cartao resumo-item">
              <span className="secundario">Gasto no mês</span>
              <strong className="num">{moeda(dados.totalRealizado)}</strong>
              <span className="secundario">de {moeda(dados.rendaBase)} de renda</span>
            </div>
            <div className="cartao resumo-item">
              <span className="secundario">Ainda resta da renda</span>
              <strong className={`num ${Number(dados.restanteDaRenda) < 0 ? 'negativo' : 'positivo'}`}>
                {moeda(dados.restanteDaRenda)}
              </strong>
              <span className="secundario">previsto no orçamento: {moeda(dados.totalPrevisto)}</span>
            </div>
            <div className="cartao resumo-item">
              <span className="secundario">Categorias que passaram</span>
              <strong className={`num ${dados.itensEstourados > 0 ? 'negativo' : 'positivo'}`}>
                {dados.itensEstourados}
              </strong>
              <span className="secundario">excedente total: {moeda(dados.totalExcedentes)}</span>
            </div>
          </section>

          <section className="cartao">
            <h2>Previsto × realizado</h2>
            <ul className="lista acompanhamento">
              {dados.itens.map((item) => (
                <li key={item.categoriaId}>
                  <div className="acompanhamento-topo">
                    <span className="principal">{item.categoriaNome}</span>
                    <span className="num">{moeda(item.realizado)} <span className="secundario-inline">de {moeda(item.previsto)}</span></span>
                    <SeloSituacao situacao={item.situacao} />
                  </div>
                  <BarraPrevistoRealizado percentual={item.percentualUtilizado} situacao={item.situacao} />
                  <p className="secundario">
                    {item.percentualUtilizado !== null && `${percentual(item.percentualUtilizado)} do previsto · `}
                    {item.situacao === 'ESTOURADO'
                      ? `Passou ${moeda(item.excedente)}. Se quiser, aumente o previsto ou reduza em outra categoria.`
                      : `Sobram ${moedaSemSinal(item.desvio)}.`}
                  </p>
                </li>
              ))}
            </ul>
          </section>

          {dados.foraDoPlano.length > 0 && (
            <section className="cartao">
              <h2>Fora do plano</h2>
              <p className="secundario">Gastos em categorias que não estão no orçamento deste mês. Contam inteiros como excedente.</p>
              <ul className="lista">
                {dados.foraDoPlano.map((gasto) => (
                  <li key={gasto.categoriaId}>
                    <span className="principal">{gasto.categoriaNome}</span>
                    <span className="num valor negativo">{moeda(gasto.realizado)}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}
    </>
  );
}
