import { useEffect, useState } from 'react';
import { api } from './api.js';
import Login from './paginas/Login.jsx';
import Lancamentos from './paginas/Lancamentos.jsx';
import Categorias from './paginas/Categorias.jsx';
import Orcamento from './paginas/Orcamento.jsx';

const PAGINAS = [
  { id: 'lancamentos', titulo: 'Lançamentos', componente: Lancamentos },
  { id: 'orcamento', titulo: 'Orçamento', componente: Orcamento },
  { id: 'categorias', titulo: 'Categorias', componente: Categorias },
];

export default function App() {
  // undefined = ainda verificando a sessão; null = sem login.
  const [usuario, setUsuario] = useState(undefined);
  const [pagina, setPagina] = useState(PAGINAS[0].id);

  useEffect(() => {
    api.get('/auth/eu').then(setUsuario).catch(() => setUsuario(null));
  }, []);

  async function sair() {
    await api.post('/auth/logout');
    setUsuario(null);
  }

  if (usuario === undefined) {
    return null;
  }
  if (usuario === null) {
    return <Login aoEntrar={setUsuario} />;
  }

  const Atual = PAGINAS.find((p) => p.id === pagina).componente;
  return (
    <>
      <header className="topo">
        <span className="marca">VGRFinance</span>
        <nav className="navegacao" aria-label="Principal">
          {PAGINAS.map((p) => (
            <button key={p.id} aria-current={p.id === pagina ? 'page' : undefined} onClick={() => setPagina(p.id)}>
              {p.titulo}
            </button>
          ))}
        </nav>
        <div className="usuario">
          <span>{usuario.nome}</span>
          <button className="discreto" onClick={sair}>Sair</button>
        </div>
      </header>
      <main className="conteudo">
        <Atual aoIrPara={setPagina} />
      </main>
    </>
  );
}
