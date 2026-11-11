// Cor nunca é o único sinal: cada situação tem ícone e rótulo.
const SITUACOES = {
  NO_PLANO: { icone: '●', rotulo: 'No plano', classe: 'positivo' },
  ATENCAO: { icone: '▲', rotulo: 'Atenção', classe: 'atencao' },
  ESTOURADO: { icone: '◆', rotulo: 'Passou', classe: 'negativo' },
};

export default function SeloSituacao({ situacao }) {
  const { icone, rotulo, classe } = SITUACOES[situacao];
  return (
    <span className={`selo-situacao ${classe}`}>
      <span aria-hidden="true">{icone}</span> {rotulo}
    </span>
  );
}
