/** Campo de moeda: prefixo R$ fixo, fonte mono, alinhado à direita, aceita vírgula. */
export default function CampoMoeda({ id, rotulo, valor, aoMudar, ref, obrigatorio = true }) {
  return (
    <div>
      <label htmlFor={id}>{rotulo}</label>
      <div className="moeda">
        <span aria-hidden="true">R$</span>
        <input
          id={id}
          ref={ref}
          inputMode="decimal"
          autoComplete="off"
          placeholder="0,00"
          required={obrigatorio}
          value={valor}
          onChange={(e) => aoMudar(e.target.value)}
        />
      </div>
    </div>
  );
}
