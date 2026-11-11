// Ícones de traço simples (24x24), herdam a cor do texto.
const TRACOS = {
  lancamentos: 'M4 7h16M4 12h16M4 17h10',
  orcamento: 'M12 3v9h9A9 9 0 1 1 12 3zM15 3.5A9 9 0 0 1 20.5 9H15z',
  acompanhamento: 'M3 17l5-5 4 4 8-8M14 8h6v6',
  categorias: 'M3 12V4h8l10 10-8 8L3 12zM7.5 7.5h.01',
  sair: 'M15 4h4v16h-4M10 8l-4 4 4 4M6 12h11',
};

export default function Icone({ nome }) {
  return (
    <svg className="icone" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"
         fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
      <path d={TRACOS[nome]} />
    </svg>
  );
}
