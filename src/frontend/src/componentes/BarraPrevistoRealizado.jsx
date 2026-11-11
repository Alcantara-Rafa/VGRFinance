/**
 * O contorno é o previsto e o preenchimento é o realizado. Quando passa de 100%,
 * o excedente aparece depois do marcador. As larguras usam o percentual que a API
 * já calculou; o navegador só desenha.
 */
function medidas(percentual, situacao) {
  // Previsto zero: a API manda percentual nulo; com gasto, tudo é excedente.
  if (percentual === null) {
    return { dentro: 0, excedente: situacao === 'ESTOURADO' ? 100 : 0, marcador: null };
  }
  const usado = Number(percentual);
  const escala = Math.max(usado, 1);
  return {
    dentro: (Math.min(usado, 1) / escala) * 100,
    excedente: usado > 1 ? ((usado - 1) / escala) * 100 : 0,
    marcador: usado > 1 ? 100 / escala : null,
  };
}

export default function BarraPrevistoRealizado({ percentual, situacao }) {
  const { dentro, excedente, marcador } = medidas(percentual, situacao);
  return (
    <div className={`barra ${situacao.toLowerCase()}`} aria-hidden="true">
      <span className="barra-dentro" style={{ width: `${dentro}%` }} />
      {excedente > 0 && <span className="barra-excedente" style={{ width: `${excedente}%` }} />}
      {marcador !== null && <span className="barra-marcador" style={{ left: `${marcador}%` }} />}
    </div>
  );
}
